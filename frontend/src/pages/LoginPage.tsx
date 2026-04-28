import { FormEvent, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../store/AuthContext';
import { ApiError } from '../api/client';

export function LoginPage() {
  const { login, register } = useAuth();
  const navigate = useNavigate();
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setLoading(true);
    setError(null);
    try {
      if (mode === 'login') {
        await login(email, password);
      } else {
        await register(email, password, name);
      }
      navigate('/projects');
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.problem.detail || err.problem.title || 'Erro');
      } else {
        setError('Erro inesperado');
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <div style={{ maxWidth: 400, margin: '4rem auto', padding: '1rem' }}>
      <h1>Task Manager</h1>
      <div style={{ marginBottom: '1rem' }}>
        <button
          type="button"
          onClick={() => setMode('login')}
          style={{ fontWeight: mode === 'login' ? 700 : 400 }}
        >
          Login
        </button>{' '}
        <button
          type="button"
          onClick={() => setMode('register')}
          style={{ fontWeight: mode === 'register' ? 700 : 400 }}
        >
          Registrar
        </button>
      </div>

      <form onSubmit={submit} style={{ display: 'grid', gap: '0.75rem' }}>
        {mode === 'register' && (
          <>
            <input
              placeholder="Nome"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
            />
            <small style={{ color: '#6b7280' }}>
              Novos usuários são criados como Membro. Apenas um Administrador pode promover outro Administrador.
            </small>
          </>
        )}
        <input
          type="email"
          placeholder="Email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
        <input
          type="password"
          placeholder="Senha (min 8)"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          minLength={8}
          required
        />
        <button type="submit" disabled={loading}>
          {loading ? 'Aguarde…' : mode === 'login' ? 'Entrar' : 'Registrar'}
        </button>
        {error && <p style={{ color: '#dc2626' }}>{error}</p>}
      </form>
    </div>
  );
}
