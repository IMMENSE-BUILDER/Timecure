const MODEL = 'minimax/minimax-m3.1-flash';

function stripThinkTags(raw: string): string {
  return raw
    .replace(/<think>[\s\S]*?<\/think>/gi, '')
    .replace(/<think>[\s\S]*$/gi, '')
    .trim();
}

export default async function handler(req: any, res: any) {
  if (req.method !== 'POST') {
    res.setHeader('Allow', 'POST');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const apiKey = process.env.AI_GATEWAY_API_KEY;
  if (!apiKey) {
    return res.status(503).json({
      error: 'AI service is not configured on the server.',
    });
  }

  let body: any = {};
  try {
    body = typeof req.body === 'string' ? JSON.parse(req.body || '{}') : req.body || {};
  } catch {
    return res.status(400).json({ error: 'Invalid JSON request body.' });
  }

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
        model: MODEL,
        temperature: 0.5,
        max_tokens: 1400,
        messages: [
          {
            role: 'system',
            content:
              'You are Timecure AI Coach (Android alarm + Pomodoro assistant). Reply concisely in 2-3 punchy, practical sentences with exact alarm time/difficulty and focus technique matching the request. Provide the answer directly in content without <think> blocks.',
          },
          { role: 'user', content: prompt },
        ],
      }),
    });

    const raw = await gatewayResp.text();
    if (!gatewayResp.ok) {
      return res.status(gatewayResp.status).json({
        error: `AI service returned HTTP ${gatewayResp.status}`,
      });
    }

    const parsed = JSON.parse(raw);
    const msg = parsed?.choices?.[0]?.message || {};
    const reply = stripThinkTags(String(msg.content || '')).slice(0, 700);

    if (!reply) {
      return res.status(502).json({ error: 'AI service returned an empty reply.' });
    }

    return res.status(200).json({ reply });
  } catch {
    return res.status(502).json({ error: 'Unable to reach AI service.' });
  }
}
