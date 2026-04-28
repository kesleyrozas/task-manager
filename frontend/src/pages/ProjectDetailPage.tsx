import { FormEvent, useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { projects, reports, tasks } from '../api/endpoints';
import { ApiError } from '../api/client';
import type { Priority, Project, ProjectReport, Status, Task } from '../types';
import { PriorityBadge } from '../components/PriorityBadge';
import { StatusBadge } from '../components/StatusBadge';

const STATUSES: Status[] = ['TODO', 'IN_PROGRESS', 'DONE'];
const PRIORITIES: Priority[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

export function ProjectDetailPage() {
  const { id } = useParams<{ id: string }>();
  const projectId = Number(id);

  const [project, setProject] = useState<Project | null>(null);
  const [taskList, setTaskList] = useState<Task[]>([]);
  const [report, setReport] = useState<ProjectReport | null>(null);
  const [error, setError] = useState<string | null>(null);

  // filtros
  const [statusFilter, setStatusFilter] = useState<Status | ''>('');
  const [search, setSearch] = useState('');

  // novo task form
  const [title, setTitle] = useState('');
  const [priority, setPriority] = useState<Priority>('MEDIUM');
  const [assigneeId, setAssigneeId] = useState<number | ''>('');

  const load = useCallback(async () => {
    if (!projectId) return;
    try {
      const [proj, page, summary] = await Promise.all([
        projects.get(projectId),
        tasks.list(projectId, { status: statusFilter || undefined, search: search || undefined, size: 50 }),
        reports.summary(projectId),
      ]);
      setProject(proj);
      setTaskList(page.content);
      setReport(summary);
    } catch (err) {
      if (err instanceof ApiError) setError(err.problem.detail || 'Erro');
    }
  }, [projectId, statusFilter, search]);

  useEffect(() => {
    load();
  }, [load]);

  async function createTask(event: FormEvent) {
    event.preventDefault();
    setError(null);
    try {
      await tasks.create(projectId, {
        title,
        priority,
        assigneeId: assigneeId === '' ? null : Number(assigneeId),
      });
      setTitle('');
      load();
    } catch (err) {
      if (err instanceof ApiError) setError(err.problem.detail || 'Erro ao criar tarefa');
    }
  }

  async function changeStatus(task: Task, next: Status) {
    setError(null);
    try {
      await tasks.update(projectId, task.id, {
        title: task.title,
        description: task.description ?? '',
        priority: task.priority,
        status: next,
        assigneeId: task.assignee?.id ?? null,
        deadline: task.deadline,
      });
      load();
    } catch (err) {
      if (err instanceof ApiError) setError(err.problem.detail || 'Erro ao atualizar tarefa');
    }
  }

  async function removeTask(task: Task) {
    setError(null);
    try {
      await tasks.remove(projectId, task.id);
      load();
    } catch (err) {
      if (err instanceof ApiError) setError(err.problem.detail || 'Erro ao remover tarefa');
    }
  }

  if (!project) {
    return (
      <div style={{ padding: '2rem' }}>
        <Link to="/projects">← Voltar</Link>
        <p>{error ?? 'Carregando…'}</p>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: 1100, margin: '2rem auto', padding: '1rem' }}>
      <Link to="/projects">← Voltar</Link>
      <h1>{project.name}</h1>
      <p style={{ color: '#64748b' }}>{project.description}</p>

      {report && (
        <div style={{ display: 'flex', gap: '2rem', margin: '1rem 0', fontSize: '0.85rem' }}>
          <div>
            <strong>Por status:</strong>{' '}
            {Object.entries(report.byStatus).map(([k, v]) => (
              <span key={k} style={{ marginRight: '0.5rem' }}>
                {k}: {v}
              </span>
            ))}
          </div>
          <div>
            <strong>Por prioridade:</strong>{' '}
            {Object.entries(report.byPriority).map(([k, v]) => (
              <span key={k} style={{ marginRight: '0.5rem' }}>
                {k}: {v}
              </span>
            ))}
          </div>
        </div>
      )}

      <h2>Nova tarefa</h2>
      <form
        onSubmit={createTask}
        style={{ display: 'grid', gap: '0.5rem', gridTemplateColumns: '2fr 1fr 1fr auto', marginBottom: '2rem' }}
      >
        <input placeholder="Título" value={title} onChange={(e) => setTitle(e.target.value)} required />
        <select value={priority} onChange={(e) => setPriority(e.target.value as Priority)}>
          {PRIORITIES.map((p) => (
            <option key={p} value={p}>
              {p}
            </option>
          ))}
        </select>
        <select value={assigneeId} onChange={(e) => setAssigneeId(e.target.value === '' ? '' : Number(e.target.value))}>
          <option value="">— sem responsável —</option>
          {project.members.map((m) => (
            <option key={m.id} value={m.id}>
              {m.name}
            </option>
          ))}
        </select>
        <button type="submit">Criar</button>
      </form>

      <h2>Tarefas</h2>
      <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem' }}>
        <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value as Status | '')}>
          <option value="">Todos status</option>
          {STATUSES.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
        <input placeholder="Busca textual" value={search} onChange={(e) => setSearch(e.target.value)} />
      </div>

      {error && <p style={{ color: '#dc2626' }}>{error}</p>}

      {taskList.length === 0 ? (
        <p>Sem tarefas para os filtros atuais.</p>
      ) : (
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr style={{ textAlign: 'left', borderBottom: '1px solid #e5e7eb' }}>
              <th>Título</th>
              <th>Status</th>
              <th>Prioridade</th>
              <th>Responsável</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {taskList.map((task) => (
              <tr key={task.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                <td>{task.title}</td>
                <td>
                  <StatusBadge status={task.status} />
                </td>
                <td>
                  <PriorityBadge priority={task.priority} />
                </td>
                <td>{task.assignee?.name ?? '—'}</td>
                <td style={{ display: 'flex', gap: '0.25rem' }}>
                  <select
                    value={task.status}
                    onChange={(e) => changeStatus(task, e.target.value as Status)}
                  >
                    {STATUSES.map((s) => (
                      <option key={s} value={s}>
                        {s}
                      </option>
                    ))}
                  </select>
                  <button onClick={() => removeTask(task)}>Excluir</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
