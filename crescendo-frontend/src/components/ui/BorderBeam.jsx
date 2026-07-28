import { motion } from "framer-motion";
import { useTheme } from "../ThemeContext";
import "./BorderBeam.css";

export const BorderBeam = ({
  className = "",
  size = 150,
  delay = 0,
  duration = 7,
  colorFrom,
  colorTo,
  style = {},
  reverse = false,
  borderWidth = 2,
}) => {
  const { theme } = useTheme();

  // Enforce high-contrast monochrome tones for crisp border visibility
  const defaultColorFrom = theme === "dark" ? "#ffffff" : "#000000";
  const defaultColorTo = theme === "dark" ? "rgba(255, 255, 255, 0.05)" : "rgba(0, 0, 0, 0.05)";

  const resolvedColorFrom = colorFrom || defaultColorFrom;
  const resolvedColorTo = colorTo || defaultColorTo;

  return (
    <div
      className={`border-beam-container ${className}`}
      style={{
        "--border-beam-width": `${borderWidth}px`,
      }}
    >
      <motion.div
        className="border-beam-element"
        style={{
          "--color-from": resolvedColorFrom,
          "--color-to": resolvedColorTo,
          ...style,
        }}
        initial={{ rotate: reverse ? 360 : 0 }}
        animate={{ rotate: reverse ? 0 : 360 }}
        transition={{
          repeat: Infinity,
          ease: "linear",
          duration,
          delay: -delay,
        }}
      />
    </div>
  );
};
