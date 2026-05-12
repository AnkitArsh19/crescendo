import { useEffect, useRef } from 'react';
import { useTheme } from './ThemeContext';
import './DotCanvas.css';

export default function DotCanvas() {
    const canvasRef = useRef(null);
    const { theme } = useTheme();
    const mouseRef = useRef({ x: -1000, y: -1000 });
    const animationRef = useRef(null);
    const dotsRef = useRef([]);

    useEffect(() => {
        const canvas = canvasRef.current;
        const ctx = canvas.getContext('2d');
        let width = window.innerWidth;
        let height = window.innerHeight;

        const resize = () => {
            width = window.innerWidth;
            height = window.innerHeight;
            canvas.width = width * window.devicePixelRatio;
            canvas.height = height * window.devicePixelRatio;
            canvas.style.width = width + 'px';
            canvas.style.height = height + 'px';
            ctx.scale(window.devicePixelRatio, window.devicePixelRatio);
            initDots();
        };

        const spacing = 50;
        const maxDots = 2000;

        function initDots() {
            const dots = [];
            const cols = Math.floor(width / spacing) + 1;
            const rows = Math.floor(height / spacing) + 1;
            const total = Math.min(cols * rows, maxDots);

            for (let i = 0; i < total; i++) {
                const col = i % cols;
                const row = Math.floor(i / cols);
                dots.push({
                    baseX: col * spacing + spacing / 2,
                    baseY: row * spacing + spacing / 2,
                    x: col * spacing + spacing / 2,
                    y: row * spacing + spacing / 2,
                    radius: Math.random() * 1.5 + 0.5,
                    opacity: Math.random() * 0.3 + 0.1,
                    pulseOffset: Math.random() * Math.PI * 2,
                    pulseSpeed: Math.random() * 0.005 + 0.002,
                });
            }
            dotsRef.current = dots;
        }

        const handleMouseMove = (e) => {
            mouseRef.current = { x: e.clientX, y: e.clientY };
        };

        const handleMouseLeave = () => {
            mouseRef.current = { x: -1000, y: -1000 };
        };

        let time = 0;

        function animate() {
            time += 1;
            ctx.clearRect(0, 0, width, height);

            const isDark = document.documentElement.getAttribute('data-theme') !== 'light';
            const mouse = mouseRef.current;
            const interactionRadius = 150;

            dotsRef.current.forEach(dot => {
                const dx = mouse.x - dot.baseX;
                const dy = mouse.y - dot.baseY;
                const dist = Math.sqrt(dx * dx + dy * dy);

                if (dist < interactionRadius) {
                    const force = (1 - dist / interactionRadius) * 25;
                    const angle = Math.atan2(dy, dx);
                    dot.x += (dot.baseX - Math.cos(angle) * force - dot.x) * 0.1;
                    dot.y += (dot.baseY - Math.sin(angle) * force - dot.y) * 0.1;
                } else {
                    dot.x += (dot.baseX - dot.x) * 0.05;
                    dot.y += (dot.baseY - dot.y) * 0.05;
                }

                const pulse = Math.sin(time * dot.pulseSpeed + dot.pulseOffset) * 0.15 + 0.85;
                const proximity = dist < interactionRadius ? 1 + (1 - dist / interactionRadius) * 2 : 1;

                const alpha = dot.opacity * pulse * proximity;
                const r = dot.radius * pulse * (proximity > 1 ? proximity * 0.6 : 1);

                if (isDark) {
                    ctx.fillStyle = `rgba(255, 255, 255, ${alpha})`;
                } else {
                    ctx.fillStyle = `rgba(0, 0, 0, ${alpha * 0.6})`;
                }

                ctx.beginPath();
                ctx.arc(dot.x, dot.y, r, 0, Math.PI * 2);
                ctx.fill();

                // Draw connecting lines between nearby dots near mouse
                if (dist < interactionRadius * 1.5) {
                    dotsRef.current.forEach(other => {
                        if (other === dot) return;
                        const ddx = dot.x - other.x;
                        const ddy = dot.y - other.y;
                        const ddist = Math.sqrt(ddx * ddx + ddy * ddy);
                        if (ddist < spacing * 1.8) {
                            const lineAlpha = (1 - ddist / (spacing * 1.8)) * 0.08 * proximity;
                            if (isDark) {
                                ctx.strokeStyle = `rgba(255, 255, 255, ${lineAlpha})`;
                            } else {
                                ctx.strokeStyle = `rgba(0, 0, 0, ${lineAlpha})`;
                            }
                            ctx.lineWidth = 0.5;
                            ctx.beginPath();
                            ctx.moveTo(dot.x, dot.y);
                            ctx.lineTo(other.x, other.y);
                            ctx.stroke();
                        }
                    });
                }
            });

            animationRef.current = requestAnimationFrame(animate);
        }

        resize();
        window.addEventListener('resize', resize);
        window.addEventListener('mousemove', handleMouseMove);
        window.addEventListener('mouseleave', handleMouseLeave);
        animate();

        return () => {
            window.removeEventListener('resize', resize);
            window.removeEventListener('mousemove', handleMouseMove);
            window.removeEventListener('mouseleave', handleMouseLeave);
            if (animationRef.current) cancelAnimationFrame(animationRef.current);
        };
    }, []);

    return <canvas ref={canvasRef} className="dot-canvas" />;
}
