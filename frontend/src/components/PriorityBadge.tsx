import type { Priority } from '../types';

const COLORS: Record<Priority, string> = {
  LOW: '#6b7280',
  MEDIUM: '#0ea5e9',
  HIGH: '#f59e0b',
  CRITICAL: '#dc2626',
};

export function PriorityBadge({ priority }: { priority: Priority }) {
  return (
    <span
      style={{
        background: COLORS[priority],
        color: 'white',
        padding: '2px 8px',
        borderRadius: '4px',
        fontSize: '0.75rem',
        fontWeight: 600,
      }}
    >
      {priority}
    </span>
  );
}
