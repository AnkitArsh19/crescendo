import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import AppSetupGuide from '../../../src/components/AppSetupGuide';

// Mock the appGuideData to return predictable content for our tests
vi.mock('../../../src/data/appGuideData', () => ({
  getAppGuide: vi.fn((appKey) => {
    if (appKey === 'no-guide') return null;
    
    return {
      category: 'Test Category',
      description: 'This is the test overview.',
      returns: 'Test Returns',
      examples: ['Example 1'],
      setupSteps: [
        { title: 'Step 1', detail: 'Do this', codeSnippet: 'Code here', link: 'https://example.com/docs' }
      ],
      outputFields: [
        { name: 'Test Field', desc: 'Test Desc' }
      ]
    };
  }),
  getCallbackUrl: vi.fn(() => 'https://app.crescendo.run/oauth/callback')
}));

describe('AppSetupGuide', () => {
  const mockOnClose = vi.fn();
  const mockOnContinue = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  const defaultApp = {
    appKey: 'test-app',
    name: 'Test App',
    category: 'Test Category'
  };

  it('renders nothing and calls onContinue immediately if no guide exists', () => {
    const appWithoutGuide = { ...defaultApp, appKey: 'no-guide' };
    const { container } = render(
      <AppSetupGuide app={appWithoutGuide} onClose={mockOnClose} onContinue={mockOnContinue} />
    );
    
    expect(container.firstChild).toBeNull();
    expect(mockOnContinue).toHaveBeenCalledTimes(1);
    expect(mockOnClose).not.toHaveBeenCalled();
  });

  it('renders correctly with guide data', () => {
    render(<AppSetupGuide app={defaultApp} onClose={mockOnClose} onContinue={mockOnContinue} />);
    
    expect(screen.getByText('Test App')).toBeInTheDocument();
    expect(screen.getByText('Test Category')).toBeInTheDocument();
    
    // Check default tab content (Overview)
    expect(screen.getByText('This is the test overview.')).toBeInTheDocument();
  });

  it('switches tabs correctly', () => {
    render(<AppSetupGuide app={defaultApp} onClose={mockOnClose} onContinue={mockOnContinue} />);
    
    // Click Setup tab
    const setupTab = screen.getByText(/Setup/i);
    fireEvent.click(setupTab);
    
    // Markdown is rendered, so we look for the text
    expect(screen.getByText('Step 1')).toBeInTheDocument();
    
    // Click Reference tab
    const referenceTab = screen.getByText(/Reference/i);
    fireEvent.click(referenceTab);
    
    expect(screen.getByText('Test Field')).toBeInTheDocument();
    expect(screen.getByText('Test Desc')).toBeInTheDocument();
  });

  it('calls onClose when close button is clicked', () => {
    render(<AppSetupGuide app={defaultApp} onClose={mockOnClose} onContinue={mockOnContinue} />);
    
    // The close button has an SVG inside it, grab by class or accessible name if possible.
    // The component has <button className="asg-close" onClick={onClose}>
    // Using container.querySelector because it doesn't have an aria-label
    const closeButton = document.querySelector('.asg-close');
    fireEvent.click(closeButton);
    
    expect(mockOnClose).toHaveBeenCalledTimes(1);
  });

  it('calls onContinue when the connect button is clicked', () => {
    render(<AppSetupGuide app={defaultApp} onClose={mockOnClose} onContinue={mockOnContinue} />);
    
    // The continue button says "Continue to Connect"
    const connectButton = screen.getByText('Continue to Connect');
    fireEvent.click(connectButton);
    
    expect(mockOnContinue).toHaveBeenCalledTimes(1);
  });
});
