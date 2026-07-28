import { describe, it, expect, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import ConditionRuleBuilder from "../../../src/pages/dashboard/nodes/ConditionRuleBuilder";

describe("ConditionRuleBuilder Component", () => {
    it("renders 'Condition Groups' header in if mode", () => {
        render(<ConditionRuleBuilder mode="if" onChange={vi.fn()} />);
        expect(screen.getByText("Condition Groups")).toBeInTheDocument();
        expect(screen.getByText("Add Condition Group (OR)")).toBeInTheDocument();
    });

    it("renders 'Routing Rules' header in switch mode", () => {
        render(<ConditionRuleBuilder mode="switch" onChange={vi.fn()} />);
        expect(screen.getByText("Routing Rules")).toBeInTheDocument();
        expect(screen.getByText("Add Routing Rule")).toBeInTheDocument();
    });

    it("clicking Add Condition Group inserts a new group and calls onChange", () => {
        const handleChange = vi.fn();
        render(<ConditionRuleBuilder mode="if" onChange={handleChange} />);

        const addGroupBtn = screen.getByText("Add Condition Group (OR)");
        fireEvent.click(addGroupBtn);

        expect(handleChange).toHaveBeenCalled();
    });

    it("renders condition rows within a group", () => {
        const initialValue = [
            {
                id: "g1",
                combinator: "AND",
                conditions: [{ id: "c1", leftValue: "status", operator: "equals", rightValue: "active" }],
            },
        ];
        render(<ConditionRuleBuilder mode="if" value={initialValue} onChange={vi.fn()} />);

        expect(screen.getByDisplayValue("status")).toBeInTheDocument();
        expect(screen.getByDisplayValue("active")).toBeInTheDocument();
    });

    it("allows adding a rule within a group in if mode", () => {
        const handleChange = vi.fn();
        const initialValue = [
            {
                id: "g1",
                combinator: "AND",
                conditions: [{ id: "c1", leftValue: "status", operator: "equals", rightValue: "active" }],
            },
        ];
        render(<ConditionRuleBuilder mode="if" value={initialValue} onChange={handleChange} />);

        const addRuleBtn = screen.getByText("Add Condition");
        fireEvent.click(addRuleBtn);

        expect(handleChange).toHaveBeenCalled();
    });

    it("toggling to JSON editor mode displays JSON textarea", () => {
        const { container } = render(<ConditionRuleBuilder mode="if" onChange={vi.fn()} />);

        const modeToggleBtn = screen.getByTitle(/Toggle Raw JSON Editor/i);
        fireEvent.click(modeToggleBtn);

        expect(container.querySelector(".cr-builder__json-area")).toBeInTheDocument();
    });
});
