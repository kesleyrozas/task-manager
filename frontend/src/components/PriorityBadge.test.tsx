import { render, screen } from '@testing-library/react';
import { PriorityBadge } from './PriorityBadge';

describe('PriorityBadge', () => {
  it('renderiza o nome da prioridade', () => {
    render(<PriorityBadge priority="HIGH" />);
    expect(screen.getByText('HIGH')).toBeInTheDocument();
  });

  it('aplica cor diferente para CRITICAL', () => {
    const { container } = render(<PriorityBadge priority="CRITICAL" />);
    const span = container.querySelector('span');
    expect(span).toHaveStyle({ background: '#dc2626' });
  });
});
