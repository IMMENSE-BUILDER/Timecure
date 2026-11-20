const ALLOWED_MODELS = new Set([
  'minimax/minimax-m3.1-flash',
  'meta/muse-ember',
]);

export default async function handler(req: any, res: any) {
  if (req.method !== 'POST') {
    res.setHeader('Allow', 'POST');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const apiKey = process.env.AI_GATEWAY_API_KEY;
  if (!apiKey) {
    return res.status(503).json({
      error: 'AI_GATEWAY_API_KEY is not configured in Vercel Environment Variables.',
    });
  }

  const body = typeof req.body === 'string' ? JSON.parse(req.body) : req.body || {};
  const model = ALLOWED_MODELS.has(body.model || '')
    ? (body.model as string)
    : 'minimax/minimax-m3.1-flash';
  const prompt = String(body.prompt || '').trim().slice(0, 300);

  if (!prompt) {
    return res.status(400).json({ error: 'Prompt is required.' });
  }

  try {
    const gatewayResp = await fetch('https://ai-gateway.vercel.sh/v1/chat/completions', {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${apiKey}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        model,
        temperature: 0.5,
        max_tokens: 220,
        messages: [
          {
            role: 'system',
            content:
              'You are Timecure AI Coach (Android alarm + Pomodoro assistant). Reply concisely in 2-3 punchy sentences: propose the exact alarm time/difficulty and focus technique matching the user request.',
          },
          { role: 'user', content: prompt },
        ],
      }),
    });

    const raw = await gatewayResp.text();
    if (!gatewayResp.ok) {
      return res.status(gatewayResp.status).json({
        error: `Gateway returned HTTP ${gatewayResp.status}`,
      });
    }

    const parsed = JSON.parse(raw);
    const reply = parsed?.choices?.[0]?.message?.content?.trim() || '';
    return res.status(200).json({ model, reply });
  } catch {
    return res.status(502).json({ error: 'Unable to reach Vercel AI Gateway.' });
  }
}
