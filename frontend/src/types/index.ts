export type Role = 'ADMIN' | 'MEMBER';
export type Status = 'TODO' | 'IN_PROGRESS' | 'DONE';
export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export const ROLE_LABEL: Record<Role, string> = {
  ADMIN: 'Administrador',
  MEMBER: 'Membro',
};

export interface AuthResponse {
  token: string;
  userId: number;
  email: string;
  name: string;
  roles: Role[];
}

export interface MemberSummary {
  id: number;
  name: string;
  email: string;
}

export interface Project {
  id: number;
  name: string;
  description: string | null;
  owner: MemberSummary;
  members: MemberSummary[];
  createdAt: string;
}

export interface Task {
  id: number;
  projectId: number;
  title: string;
  description: string | null;
  status: Status;
  priority: Priority;
  assignee: MemberSummary | null;
  deadline: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  total: number;
  totalPages: number;
}

export interface ProjectReport {
  projectId: number;
  byStatus: Record<string, number>;
  byPriority: Record<string, number>;
}

export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  errors?: Record<string, string>;
}
