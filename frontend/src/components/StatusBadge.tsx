import type { Status } from '../types';

const COLORS: Record<Status, string> = {
  TODO: '#94a3b8',
  IN_PROGRESS: '#0ea5e9',
  DONE: '#16a34a',
};

export function StatusBadge({ status }: { status: Status }) {
  return (
    <span
      style={{
        background: COLORS[status],
        color: 'white',
        padding: '2px 8px',
        borderRadius: '4px',
        fontSize: '0.75rem',
        fontWeight: 600,
      }}
    >
      {status.replace('_', ' ')}
    </span>
  );
}
