import React, { useRef } from 'react';
import { motion, useMotionValue, useSpring, useTransform } from 'framer-motion';
import './Dock.css';

const DEFAULT_SIZE = 36;
const DEFAULT_MAGNIFICATION = 48;
const DEFAULT_DISTANCE = 130;

export const Dock = React.forwardRef(({
    className = '',
    children,
    iconSize = DEFAULT_SIZE,
    iconMagnification = DEFAULT_MAGNIFICATION,
    disableMagnification = false,
    iconDistance = DEFAULT_DISTANCE,
    direction = "middle",
    ...props
}, ref) => {
    const mouseX = useMotionValue(Infinity);

    const renderChildren = () => {
        return React.Children.map(children, (child) => {
            if (React.isValidElement(child) && (child.type === DockIcon || child.type?.displayName === 'DockIcon')) {
                return React.cloneElement(child, {
                    ...child.props,
                    mouseX: mouseX,
                    size: iconSize,
                    magnification: iconMagnification,
                    disableMagnification: disableMagnification,
                    distance: iconDistance,
                });
            }
            return child;
        });
    };

    return (
        <motion.div
            ref={ref}
            onMouseMove={(e) => mouseX.set(e.pageX)}
            onMouseLeave={() => mouseX.set(Infinity)}
            {...props}
            className={`dock-container ${direction} ${className}`.trim()}
        >
            {renderChildren()}
        </motion.div>
    );
});

Dock.displayName = 'Dock';

export const DockIcon = ({
    size = DEFAULT_SIZE,
    magnification = DEFAULT_MAGNIFICATION,
    disableMagnification,
    distance = DEFAULT_DISTANCE,
    mouseX,
    className = '',
    children,
    onClick,
    disabled = false,
    ...props
}) => {
    const ref = useRef(null);
    const defaultMouseX = useMotionValue(Infinity);

    const distanceCalc = useTransform(mouseX ?? defaultMouseX, (val) => {
        const bounds = ref.current?.getBoundingClientRect() ?? { x: 0, width: 0 };
        return val - bounds.x - bounds.width / 2;
    });

    const targetHeight = (disableMagnification || disabled) ? size : magnification;
    const targetFontSize = (disableMagnification || disabled) ? 13.5 : 17;
    const targetPaddingX = (disableMagnification || disabled) ? 14 : 20;

    const heightTransform = useTransform(
        distanceCalc,
        [-distance, 0, distance],
        [size, targetHeight, size]
    );

    const fontSizeTransform = useTransform(
        distanceCalc,
        [-distance, 0, distance],
        [13.5, targetFontSize, 13.5]
    );

    const paddingXTransform = useTransform(
        distanceCalc,
        [-distance, 0, distance],
        [14, targetPaddingX, 14]
    );

    const springConfig = {
        mass: 0.1,
        stiffness: 150,
        damping: 12,
    };

    const scaleHeight = useSpring(heightTransform, springConfig);
    const scaleFontSize = useSpring(fontSizeTransform, springConfig);
    const scalePaddingX = useSpring(paddingXTransform, springConfig);

    return (
        <motion.div
            ref={ref}
            style={{
                height: scaleHeight,
                fontSize: scaleFontSize,
                paddingLeft: scalePaddingX,
                paddingRight: scalePaddingX,
            }}
            className={`dock-icon ${disabled ? 'disabled' : ''} ${className}`.trim()}
            onClick={disabled ? undefined : onClick}
            {...props}
        >
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', width: '100%', height: '100%' }}>
                {children}
            </div>
        </motion.div>
    );
};

DockIcon.displayName = 'DockIcon';
