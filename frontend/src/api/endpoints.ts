import { apiRequest } from './client';
import type {
  AuthResponse,
  Page,
  Priority,
  Project,
  ProjectReport,
  Status,
  Task,
} from '../types';

export const auth = {
  login: (email: string, password: string) =>
    apiRequest<AuthResponse>('/v1/auth/login', {
      method: 'POST',
      body: { email, password },
    }),
  register: (email: string, password: string, name: string) =>
    apiRequest<AuthResponse>('/v1/auth/register', {
      method: 'POST',
      body: { email, password, name },
    }),
};

export const projects = {
  list: () => apiRequest<Project[]>('/v1/projects'),
  get: (id: number) => apiRequest<Project>(`/v1/projects/${id}`),
  create: (name: string, description: string) =>
    apiRequest<Project>('/v1/projects', {
      method: 'POST',
      body: { name, description },
    }),
  addMember: (projectId: number, userId: number) =>
    apiRequest<Project>(`/v1/projects/${projectId}/members`, {
      method: 'POST',
      body: { userId },
    }),
};

export type TaskFilters = {
  [key: string]: string | number | undefined;
  status?: Status;
  priority?: Priority;
  assigneeId?: number;
  search?: string;
  page?: number;
  size?: number;
  sort?: string;
};

export const tasks = {
  list: (projectId: number, filters: TaskFilters = {}) =>
    apiRequest<Page<Task>>(`/v1/projects/${projectId}/tasks`, { query: filters }),
  create: (projectId: number, body: {
    title: string;
    description?: string;
    priority: Priority;
    assigneeId?: number | null;
    deadline?: string | null;
  }) =>
    apiRequest<Task>(`/v1/projects/${projectId}/tasks`, {
      method: 'POST',
      body,
    }),
  update: (projectId: number, taskId: number, body: {
    title: string;
    description?: string;
    priority: Priority;
    status: Status;
    assigneeId?: number | null;
    deadline?: string | null;
  }) =>
    apiRequest<Task>(`/v1/projects/${projectId}/tasks/${taskId}`, {
      method: 'PUT',
      body,
    }),
  remove: (projectId: number, taskId: number) =>
    apiRequest<void>(`/v1/projects/${projectId}/tasks/${taskId}`, {
      method: 'DELETE',
    }),
};

export const reports = {
  summary: (projectId: number) =>
    apiRequest<ProjectReport>(`/v1/projects/${projectId}/report`),
};
