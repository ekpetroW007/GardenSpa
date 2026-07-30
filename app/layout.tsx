import type { Metadata } from "next";
import { headers } from "next/headers";
import "./globals.css";

const title = "GardenSpa — ваш сад в своём ритме";
const description =
  "Персональный Android-помощник для садовода: растения, процедуры, календарь ухода и препараты в одном спокойном месте.";

export async function generateMetadata(): Promise<Metadata> {
  const requestHeaders = await headers();
  const host = requestHeaders.get("x-forwarded-host") ?? requestHeaders.get("host");
  const protocol = requestHeaders.get("x-forwarded-proto") ?? "https";
  const origin = host ? `${protocol}://${host}` : "https://gardenspa.example";

  return {
    title,
    description,
    keywords: ["GardenSpa", "сад", "растения", "календарь ухода", "Android"],
    icons: {
      icon: "/icon.png",
      shortcut: "/icon.png",
      apple: "/icon.png",
    },
    openGraph: {
      title,
      description: "Спокойный помощник для тех, кто растит с любовью.",
      type: "website",
      locale: "ru_RU",
      images: [
        {
          url: `${origin}/og.png`,
          width: 1732,
          height: 909,
          alt: "GardenSpa — ваш сад в своём ритме",
        },
      ],
    },
    twitter: {
      card: "summary_large_image",
      title,
      description: "Спокойный помощник для тех, кто растит с любовью.",
      images: [`${origin}/og.png`],
    },
  };
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ru">
      <body>{children}</body>
    </html>
  );
}
