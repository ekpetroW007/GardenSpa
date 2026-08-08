import assert from "node:assert/strict";
import { access, readFile } from "node:fs/promises";
import test from "node:test";

const templateRoot = new URL("../", import.meta.url);

async function render() {
  const workerUrl = new URL("../dist/server/index.js", import.meta.url);
  workerUrl.searchParams.set("test", `${process.pid}-${Date.now()}`);
  const { default: worker } = await import(workerUrl.href);

  return worker.fetch(
    new Request("http://localhost/", {
      headers: { accept: "text/html", host: "localhost" },
    }),
    {
      ASSETS: {
        fetch: async () => new Response("Not found", { status: 404 }),
      },
    },
    {
      waitUntil() {},
      passThroughOnException() {},
    },
  );
}

test("server-renders the GardenSpa landing page", async () => {
  const response = await render();
  assert.equal(response.status, 200);
  assert.match(response.headers.get("content-type") ?? "", /^text\/html\b/i);

  const html = await response.text();
  assert.match(html, /<title>GardenSpa[^<]*приложение для садовода[^<]*<\/title>/i);
  assert.match(html, /Приложение для ухода/);
  assert.match(html, /Календарь ухода/);
  assert.match(html, /Только на вашем телефоне/);
  assert.match(html, /Android-приложение для садоводов и дачников/);
  assert.match(html, /Садовый дневник/);
  assert.match(html, /https:\/\/www\.rustore\.ru\/catalog\/app\/ru\.samates\.gardenspa/);
  assert.match(html, /Скачать в RuStore/);
  assert.match(html, /\/rustore-gardenspa-qr\.png/);
  assert.match(html, /\/screenshots\/app-home-v1-0-24\.png/);
  assert.match(html, /\/screenshots\/app-calendar\.png/);
  assert.match(html, /\/og\.png/);
  assert.doesNotMatch(html, /codex-preview|SkeletonPreview|Starter Project/);
});

test("removes starter UI and keeps the finished metadata", async () => {
  const [page, layout, css, packageJson] = await Promise.all([
    readFile(new URL("../app/page.tsx", import.meta.url), "utf8"),
    readFile(new URL("../app/layout.tsx", import.meta.url), "utf8"),
    readFile(new URL("../app/globals.css", import.meta.url), "utf8"),
    readFile(new URL("../package.json", import.meta.url), "utf8"),
  ]);

  assert.match(page, /GardenSpa/);
  assert.match(page, /id="privacy"/);
  assert.match(layout, /generateMetadata/);
  assert.match(layout, /\/og\.png/);
  assert.match(css, /prefers-reduced-motion:\s*reduce/);
  assert.doesNotMatch(packageJson, /react-loading-skeleton/);

  await assert.rejects(
    access(new URL("../app/_sites-preview/SkeletonPreview.tsx", import.meta.url)),
  );
  await access(new URL("public/og.png", templateRoot));
  await access(new URL("public/rustore-gardenspa-qr.png", templateRoot));
});
