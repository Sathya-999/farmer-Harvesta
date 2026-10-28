import type { Config } from 'tailwindcss'

const config: Config = {
  content: [
    './pages/**/*.{js,ts,jsx,tsx,mdx}',
    './components/**/*.{js,ts,jsx,tsx,mdx}',
    './app/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        'soil': {
          950: '#1A1408',
          900: '#241C0B',
          800: '#3A2D12',
        },
        'canopy': {
          700: '#2F5233',
          600: '#3D6B42',
          500: '#4D8352',
        },
        'moss': {
          400: '#7A9B5C',
          300: '#9AB87A',
        },
        'sun': {
          400: '#F2C84B',
          300: '#F5D87A',
          200: '#FAE9B0',
        },
        'dawn': {
          100: '#FBF4E4',
          50:  '#FEFCF6',
        },
        'bark': {
          600: '#6B4E3D',
          400: '#9A7562',
          200: '#C9B09E',
        },
      },
      fontFamily: {
        'display': ['Plus Jakarta Sans', 'system-ui', 'sans-serif'],
        'brand':   ['Fraunces', 'Georgia', 'serif'],
        'body':    ['Inter', 'system-ui', 'sans-serif'],
        'mono':    ['Fragment Mono', 'Courier New', 'monospace'],
      },
      borderRadius: {
        'organic': '24px',
        'soft': '14px',
      },
      transitionTimingFunction: {
        'settle': 'cubic-bezier(0.22, 1, 0.36, 1)',
        'bounce': 'cubic-bezier(0.34, 1.56, 0.64, 1)',
      },
      backgroundImage: {
        'horizon': 'linear-gradient(to bottom, #87CEEB 0%, #FBF4E4 30%, #FBF4E4 100%)',
        'field-gradient': 'linear-gradient(145deg, #4D8352, #2F5233)',
        'sun-sweep': 'linear-gradient(90deg, transparent, rgba(242,200,75,0.25), transparent)',
      },
      animation: {
        'breathe': 'breathe 5s ease-in-out infinite',
        'sway': 'sway 6s ease-in-out infinite',
        'grass': 'grassSway 3s ease-in-out infinite',
        'bloom': 'bloom 0.5s cubic-bezier(0.34, 1.56, 0.64, 1) forwards',
        'leaf-fall': 'leafFall 0.6s ease-in forwards',
        'draw-line': 'drawLine 1.5s ease-out forwards',
      },
    },
  },
  plugins: [],
}

export default config
