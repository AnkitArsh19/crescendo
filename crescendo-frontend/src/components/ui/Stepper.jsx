import { HiCheck } from 'react-icons/hi';
import './Stepper.css';

export default function Stepper({ steps, currentStep }) {
    return (
        <div className="stepper">
            {steps.map((step, i) => {
                const isDone = i < currentStep;
                const isActive = i === currentStep;
                return (
                    <div className="stepper-step" key={i}>
                        <div className="stepper-step-content">
                            <div className={`stepper-circle ${isActive ? 'active' : ''} ${isDone ? 'done' : ''}`}>
                                {isDone ? <HiCheck /> : i + 1}
                            </div>
                            <span className={`stepper-label ${isActive ? 'active' : ''} ${isDone ? 'done' : ''}`}>
                                {step}
                            </span>
                        </div>
                        {i < steps.length - 1 && (
                            <div className={`stepper-line ${isDone ? 'done' : ''}`} />
                        )}
                    </div>
                );
            })}
        </div>
    );
}
