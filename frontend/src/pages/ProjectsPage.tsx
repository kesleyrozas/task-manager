import { FormEvent, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { projects } from '../api/endpoints';
import { useAuth } from '../store/AuthContext';
import { ApiError } from '../api/client';
import { ROLE_LABEL, type Project } from '../types';

export function ProjectsPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [items, setItems] = useState<Project[]>([]);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const isAdmin = user?.roles.includes('ADMIN');

  useEffect(() => {
    if (!user) {
      navigate('/login');
      return;
    }
    projects
      .list()
      .then(setItems)
      .catch((err) => {
        if (err instanceof ApiError) setError(err.problem.detail || 'Erro ao listar projetos');
      })
      .finally(() => setLoading(false));
  }, [user, navigate]);

  async function createProject(event: FormEvent) {
    event.preventDefault();
    setError(null);
    try {
      const created = await projects.create(name, description);
      setItems((prev) => [...prev, created]);
      setName('');
      setDescription('');
    } catch (err) {
      if (err instanceof ApiError) setError(err.problem.detail || 'Erro ao criar projeto');
    }
  }

  return (
    <div style={{ maxWidth: 900, margin: '2rem auto', padding: '1rem' }}>
      <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h1>Projetos</h1>
        <div>
          <span style={{ marginRight: '1rem' }}>
            {user?.name} ({user?.roles.map((r) => ROLE_LABEL[r]).join(', ')})
          </span>
          <button onClick={logout}>Sair</button>
        </div>
      </header>

      {isAdmin && (
        <form
          onSubmit={createProject}
          style={{ display: 'grid', gap: '0.5rem', marginBottom: '2rem', gridTemplateColumns: '1fr 2fr auto' }}
        >
          <input
            placeholder="Nome do projeto"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />
          <input
            placeholder="Descrição"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
          <button type="submit">Criar</button>
        </form>
      )}

      {error && <p style={{ color: '#dc2626' }}>{error}</p>}
      {loading ? (
        <p>Carregando…</p>
      ) : items.length === 0 ? (
        <p>Você ainda não tem projetos.</p>
      ) : (
        <ul style={{ listStyle: 'none', padding: 0 }}>
          {items.map((project) => (
            <li
              key={project.id}
              style={{ border: '1px solid #e5e7eb', padding: '1rem', marginBottom: '0.5rem', borderRadius: 4 }}
            >
              <Link to={`/projects/${project.id}`} style={{ fontWeight: 600 }}>
                {project.name}
              </Link>
              <p style={{ color: '#64748b', margin: '4px 0' }}>{project.description}</p>
              <small>
                Owner: {project.owner.name} · {project.members.length} membro(s)
              </small>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
