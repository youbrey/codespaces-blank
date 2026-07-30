import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { google } from "googleapis";

admin.initializeApp();
const db = admin.firestore();

const PACKAGE_NAME = "com.docapp.editor";

// Bit mask per produk, selaras dengan FeatureIds di client (core/gate).
const PRODUCT_MASK: Record<string, number> = {
  pro_bundle_onetime: (1 << 3) | (1 << 9) | (1 << 5), // K7 + M2 + R4
  essential_monthly: 1 << 5,                           // R4 saja
};

const ANOMALY_THRESHOLD = 20;       // device unik per token dalam window
const ANOMALY_WINDOW_MS = 24 * 60 * 60 * 1000;

async function getAndroidPublisher() {
  const auth = new google.auth.GoogleAuth({
    scopes: ["https://www.googleapis.com/auth/androidpublisher"],
  });
  return google.androidpublisher({ version: "v3", auth });
}

async function isTokenAnomalous(token: string, deviceId: string): Promise<boolean> {
  const ref = db.collection("device_bindings").doc(token);
  const now = Date.now();

  await ref.set(
    { devices: admin.firestore.FieldValue.arrayUnion({ deviceId, ts: now }) },
    { merge: true }
  );

  const snap = await ref.get();
  const devices: { deviceId: string; ts: number }[] = snap.data()?.devices ?? [];
  const recentUnique = new Set(
    devices.filter((d) => now - d.ts < ANOMALY_WINDOW_MS).map((d) => d.deviceId)
  );
  return recentUnique.size > ANOMALY_THRESHOLD;
}

/**
 * Proxy generate dokumen via Gemini API. Wajib Authorization: Bearer <Google ID Token>
 * dari login akun Google di client — tanpa token valid, request ditolak (401).
 * API key Gemini disimpan sebagai Firebase Functions secret, tidak pernah dikirim ke client.
 */
export const generateDocument = functions
  .region("asia-southeast1")
  .https.onRequest(async (req, res) => {
    try {
      const authHeader = req.headers.authorization ?? "";
      const idToken = authHeader.replace("Bearer ", "");
      if (!idToken) {
        res.status(401).json({ error: "Login akun Google diperlukan" });
        return;
      }

      await admin.auth().verifyIdToken(idToken).catch(() => {
        throw new Error("unauthorized");
      });

      const prompt = req.body?.prompt as string;
      if (!prompt) {
        res.status(400).json({ error: "Prompt kosong" });
        return;
      }

      const geminiKey = process.env.GEMINI_API_KEY;
      const geminiRes = await fetch(
        `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${geminiKey}`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            contents: [{
              parts: [{
                text: `Buat dokumen berdasarkan instruksi berikut. Balas HANYA JSON valid format {"title": "...", "paragraphs": ["...","..."]}. Instruksi: ${prompt}`,
              }],
            }],
          }),
        }
      );
      const geminiJson: any = await geminiRes.json();
      const rawText = geminiJson.candidates?.[0]?.content?.parts?.[0]?.text ?? "{}";
      const cleaned = rawText.replace(/```json|```/g, "").trim();
      const parsed = JSON.parse(cleaned);

      res.json(parsed);
    } catch (err) {
      console.error("generateDocument error", err);
      res.status(err instanceof Error && err.message === "unauthorized" ? 401 : 500)
        .json({ error: "Gagal generate dokumen" });
    }
  });

export const verifyPurchase = functions
  .region("asia-southeast1")
  .https.onRequest(async (req, res) => {
    try {
      const { purchaseToken, productId, packageName, deviceId } = req.body;

      if (!purchaseToken || !productId || packageName !== PACKAGE_NAME) {
        res.status(400).json({ valid: false, mask: 0, expiresAt: 0 });
        return;
      }

      const anomalous = await isTokenAnomalous(purchaseToken, deviceId);
      if (anomalous) {
        res.json({ valid: false, mask: 0, expiresAt: 0 });
        return;
      }

      const publisher = await getAndroidPublisher();
      const isSubscription = productId === "essential_monthly";

      let valid = false;
      if (isSubscription) {
        const result = await publisher.purchases.subscriptions.get({
          packageName, subscriptionId: productId, token: purchaseToken,
        });
        valid = result.data.paymentState === 1; // 1 = received
      } else {
        const result = await publisher.purchases.products.get({
          packageName, productId, token: purchaseToken,
        });
        valid = result.data.purchaseState === 0; // 0 = purchased
      }

      const mask = valid ? PRODUCT_MASK[productId] ?? 0 : 0;
      const expiresAt = Date.now() + 7 * 24 * 60 * 60 * 1000; // re-verify tiap 7 hari

      res.json({ valid, mask, expiresAt });
    } catch (err) {
      console.error("verifyPurchase error", err);
      res.status(500).json({ valid: false, mask: 0, expiresAt: 0 });
    }
  });
