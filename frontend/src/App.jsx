import { useEffect, useRef, useState } from 'react';

const GREETING = { role: 'assistant', text: 'Hi! How can I help you today?' };

export default function App() {
  const [messages, setMessages] = useState([GREETING]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [conversationId, setConversationId] = useState('');
  const endRef = useRef(null);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, loading]);

  async function send(e) {
    e.preventDefault();
    const text = input.trim();
    if (!text || loading) return;
    setInput('');
    setMessages((m) => [...m, { role: 'user', text }]);
    setLoading(true);
    try {
      const res = await fetch('/api/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ conversationId, message: text }),
      });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      setConversationId(data.conversationId);
      setMessages((m) => [
        ...m,
        {
          role: 'assistant',
          text: data.answer,
          confidence: data.confidence,
          escalated: data.escalated,
          sources: data.sources,
        },
      ]);
    } catch {
      setMessages((m) => [
        ...m,
        { role: 'assistant', text: 'Sorry, something went wrong. Please try again.', error: true },
      ]);
    } finally {
      setLoading(false);
    }
  }

  function newChat() {
    setConversationId('');
    setMessages([GREETING]);
  }

  return (
    <div className="chat">
      <header>
        <span>Customer Support</span>
        <button onClick={newChat}>New chat</button>
      </header>
      <div className="messages">
        {messages.map((m, i) => (
          <div key={i} className={`msg ${m.role}${m.error ? ' error' : ''}`}>
            <div className="bubble">{m.text}</div>
            {m.confidence !== undefined && (
              <div className="meta">
                {m.escalated
                  ? 'Escalated to a human agent'
                  : `Confidence ${(m.confidence * 100).toFixed(0)}% · ${[
                      ...new Set(m.sources.map((s) => s.title)),
                    ]
                      .slice(0, 2)
                      .join(', ')}`}
              </div>
            )}
          </div>
        ))}
        {loading && (
          <div className="msg assistant">
            <div className="bubble">…</div>
          </div>
        )}
        <div ref={endRef} />
      </div>
      <form onSubmit={send}>
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Type your question…"
          autoFocus
        />
        <button type="submit" disabled={loading || !input.trim()}>
          Send
        </button>
      </form>
    </div>
  );
}
