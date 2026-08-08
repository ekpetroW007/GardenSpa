import type { Metadata } from "next";
import { headers } from "next/headers";
import "./globals.css";

const title = "GardenSpa — приложение для садовода и ухода за растениями";
const description =
  "GardenSpa — Android-приложение для садоводов: дневник растений, календарь полива, подкормок и обработок, напоминания и учёт препаратов. Работает офлайн.";

export async function generateMetadata(): Promise<Metadata> {
  const requestHeaders = await headers();
  const host = requestHeaders.get("x-forwarded-host") ?? requestHeaders.get("host");
  const protocol = requestHeaders.get("x-forwarded-proto") ?? "https";
  const origin = host ? `${protocol}://${host}` : "https://gardenspa.example";

  return {
    title,
    description,
    icons: {
      icon: "/gardenspa-app-icon-v2.png",
      shortcut: "/gardenspa-app-icon-v2.png",
      apple: "/gardenspa-app-icon-v2.png",
    },
    openGraph: {
      title,
      description: "Садовый дневник, календарь ухода за растениями и напоминания в одном Android-приложении.",
      type: "website",
      locale: "ru_RU",
      images: [
        {
          url: `${origin}/og.png`,
          width: 1732,
          height: 909,
          alt: "GardenSpa — приложение для садовода и ухода за растениями",
        },
      ],
    },
    twitter: {
      card: "summary_large_image",
      title,
      description: "Садовый дневник, календарь ухода за растениями и напоминания в одном Android-приложении.",
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
