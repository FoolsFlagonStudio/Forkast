import { Link } from 'react-router-dom';
import { Logo } from '../brand/Logo';

// Placeholder until the marketing site designs come over from Figma.
export default function Home() {
  return (
    <main style={{ display: 'grid', placeItems: 'center', minHeight: '100vh', gap: 'var(--space-lg)', padding: 'var(--space-lg)' }}>
      <div style={{ display: 'grid', justifyItems: 'center', gap: 'var(--space-lg)', textAlign: 'center' }}>
        <Logo height={56} />
        <p className="type-h3">Meals planned to your budget</p>
        <Link to="/styleguide" className="type-small" style={{ color: 'var(--color-primary)' }}>
          View style guide
        </Link>
      </div>
    </main>
  );
}
