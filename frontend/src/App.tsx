import { lazy, Suspense } from 'react';
import { Route, Routes } from 'react-router-dom';
import Home from './pages/Home';

// Dev reference for checking tokens against the Figma. Lazy so it stays out of the main bundle.
const StyleGuide = lazy(() => import('./pages/StyleGuide'));

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route
        path="/styleguide"
        element={
          <Suspense fallback={null}>
            <StyleGuide />
          </Suspense>
        }
      />
    </Routes>
  );
}
