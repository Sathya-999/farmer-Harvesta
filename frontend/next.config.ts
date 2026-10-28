import type { NextConfig } from 'next'

const nextConfig: NextConfig = {
  // Disable Next.js dev toolbar (the "N" overlay at bottom-left in dev mode)
  devIndicators: false,

  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: `${process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080'}/api/:path*`,
      },
    ]
  },
}

export default nextConfig
