const RUSTORE_URL = "https://www.rustore.ru/catalog/app/ru.samates.gardenspa";

const features = [
  {
    number: "01",
    title: "Погодное окно",
    text: "GardenSpa сопоставляет прогноз с ближайшей работой и показывает, подходит ли погода для обработки, полива или другого ухода.",
    accent: "leaf",
  },
  {
    number: "02",
    title: "Несколько садов",
    text: "Ведите дачу, теплицу и домашние растения отдельно. Ближайшая работа и прогноз выбираются для нужного сада.",
    accent: "sun",
  },
  {
    number: "03",
    title: "Календарь и программы",
    text: "Используйте готовые программы ухода, планируйте процедуры и препараты, переносите работу и отмечайте выполненное.",
    accent: "drop",
  },
];

const steps = [
  {
    number: "1",
    title: "Добавьте свои сады",
    text: "Создайте дачу, теплицу или домашнюю коллекцию и укажите их местоположение для прогноза.",
  },
  {
    number: "2",
    title: "Выберите растения и уход",
    text: "Найдите растение в подсказках, подключите готовую программу или назначьте собственную работу.",
  },
  {
    number: "3",
    title: "Получайте рекомендации",
    text: "GardenSpa соберёт работы в календаре и предупредит, если дождь, ветер или температура мешают процедуре.",
  },
];

const videoChapters = [
  { time: "00:00", label: "Погодное окно для ближайшей работы" },
  { time: "00:13", label: "План садовых дел на сегодня" },
  { time: "00:19", label: "Несколько садов и растения" },
  { time: "00:29", label: "Календарь запланированных работ" },
  { time: "00:35", label: "Справочник садовода" },
];

const screenshots = [
  {
    src: "/screenshots/app-today-weather-v1-0-39.png",
    title: "Погодное окно",
    text: "Рекомендация для ближайшей работы и безопасный интервал без дождя.",
  },
  {
    src: "/screenshots/app-gardens-v1-0-39.png",
    title: "Мои сады",
    text: "Дача, теплица и растения — каждый со своим планом ухода.",
  },
  {
    src: "/screenshots/app-calendar-v1-0-39.png",
    title: "Календарь",
    text: "Работы, препараты, повторы и быстрые действия в одном экране.",
  },
  {
    src: "/screenshots/app-reference-v1-0-39.png",
    title: "Справочник",
    text: "Средства обработки и практические рецепты для садовых задач.",
  },
  {
    src: "/screenshots/app-plant-search-v1-0-39.png",
    title: "Поиск растений",
    text: "Большой прокручиваемый список подсказок прямо под строкой ввода.",
  },
];

function BrandMark() {
  return (
    <span className="brand-mark" aria-hidden="true">
      <span className="brand-leaf brand-leaf-left" />
      <span className="brand-leaf brand-leaf-right" />
    </span>
  );
}

function ArrowIcon() {
  return <span className="arrow" aria-hidden="true">↗</span>;
}

function PhonePreview({
  compact = false,
  screen = "today",
}: {
  compact?: boolean;
  screen?: "today" | "calendar";
}) {
  const isCalendar = screen === "calendar";

  return (
    <div className={compact ? "phone phone-compact" : "phone"}>
      <div className="phone-frame phone-frame-real">
        <img
          className="phone-screenshot"
          src={
            isCalendar
              ? "/screenshots/app-calendar-v1-0-39.png"
              : "/screenshots/app-today-weather-v1-0-39.png"
          }
          alt={
            isCalendar
              ? "Календарь ухода за растениями в приложении GardenSpa"
              : "Погодное окно для ближайшей работы в приложении GardenSpa"
          }
          width={1080}
          height={2400}
          loading={compact ? "lazy" : "eager"}
        />
      </div>
    </div>
  );
}

export default function Home() {
  return (
    <main>
      <section className="hero" id="top">
        <div className="hero-image" />
        <div className="hero-shade" />
        <nav className="nav shell" aria-label="Основная навигация">
          <a href="#top" className="brand" aria-label="GardenSpa — на главную">
            <BrandMark />
            <span>Garden<span>Spa</span></span>
          </a>
          <div className="nav-links">
            <a href="#possibilities">Возможности</a>
            <a href="#screens">Экраны</a>
            <a href="#video">Видео</a>
            <a href="#privacy">Приватность</a>
          </div>
          <a
            className="nav-qr"
            href={RUSTORE_URL}
            target="_blank"
            rel="noopener noreferrer"
            aria-label="Открыть страницу GardenSpa в RuStore"
          >
            <img
              src="/rustore-gardenspa-qr.png"
              alt="QR-код GardenSpa в RuStore"
              width={630}
              height={630}
            />
          </a>
          <a
            href={RUSTORE_URL}
            className="nav-cta"
            target="_blank"
            rel="noopener noreferrer"
          >
            Скачать <ArrowIcon />
          </a>
        </nav>

        <div className="hero-content shell">
          <div className="hero-copy">
            <div className="kicker"><span /> Android-приложение для садоводов и дачников</div>
            <h1>Приложение для ухода<br /><em>за садом и растениями.</em></h1>
            <p>
              GardenSpa объединяет несколько садов, готовые программы ухода,
              календарь и прогноз погоды. Приложение подскажет, когда работу
              лучше выполнить, а когда перенести из-за дождя, ветра или холода.
            </p>
            <div className="hero-actions">
              <a
                className="button button-primary"
                href={RUSTORE_URL}
                target="_blank"
                rel="noopener noreferrer"
              >
                Скачать в RuStore <ArrowIcon />
              </a>
              <a className="video-link" href="#video">Смотреть видео <span aria-hidden="true">↓</span></a>
            </div>
          </div>
          <div className="hero-phone">
            <div className="phone-aura" />
            <PhonePreview screen="today" />
            <div className="floating-note floating-note-top">
              <span className="note-icon note-icon-leaf" />
              <span><small>Погодное окно</small><strong>Условия подходят</strong></span>
            </div>
            <div className="floating-note floating-note-bottom">
              <span className="pulse-dot" />
              <span><small>Ближайшая работа</small><strong>Для нужного сада</strong></span>
            </div>
          </div>
        </div>

        <div className="hero-foot shell">
          <span>Погодные рекомендации</span><i />
          <span>Несколько садов</span><i />
          <span>Календарь ухода</span><i />
          <span>Готовые программы</span>
        </div>
      </section>

      <section className="intro section-light">
        <div className="shell intro-grid">
          <div>
            <span className="section-label">Уход без перегруза</span>
            <h2>Календарь ухода за растениями — <em>в одном спокойном месте.</em></h2>
          </div>
          <div className="intro-copy">
            <p>
              Когда растений становится больше, трудно помнить, что и когда
              поливать, чем подкармливать и безопасно ли проводить обработку.
              GardenSpa превращает разрозненные дела в понятный план с учётом погоды.
            </p>
            <div className="intro-facts">
              <span><b>01</b> План сада доступен офлайн</span>
              <span><b>02</b> Без рекламы</span>
              <span><b>03</b> Прогноз для конкретной работы</span>
            </div>
          </div>
        </div>
      </section>

      <section className="possibilities section-light" id="possibilities">
        <div className="shell">
          <div className="section-head">
            <span className="section-label">Возможности приложения для садовода</span>
            <p>Дневник растений, календарь работ и препараты в одной системе</p>
          </div>
          <div className="feature-grid">
            {features.map((feature) => (
              <article className={`feature-card feature-${feature.accent}`} key={feature.number}>
                <div className="feature-top">
                  <span className="feature-number">{feature.number}</span>
                  <span className="feature-art" aria-hidden="true"><i /><b /></span>
                </div>
                <h3>{feature.title}</h3>
                <p>{feature.text}</p>
                <a href="#how">Подробнее <ArrowIcon /></a>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="screens section-light" id="screens">
        <div className="shell">
          <div className="screens-head">
            <span className="section-label">Актуальная версия GardenSpa</span>
            <h2>Пять экранов — <em>от прогноза до выполненной работы.</em></h2>
            <p>Настоящий интерфейс версии 1.0.39 с заполненным садом, календарём и погодной рекомендацией.</p>
          </div>
          <div className="screens-track">
            {screenshots.map((screenshot) => (
              <figure className="screen-card" key={screenshot.src}>
                <div className="screen-device">
                  <img
                    src={screenshot.src}
                    alt={`${screenshot.title} в приложении GardenSpa`}
                    width={1080}
                    height={2400}
                    loading="lazy"
                  />
                </div>
                <figcaption>
                  <strong>{screenshot.title}</strong>
                  <span>{screenshot.text}</span>
                </figcaption>
              </figure>
            ))}
          </div>
        </div>
      </section>

      <section className="video-guide" id="video">
        <div className="shell video-guide-grid">
          <div className="video-guide-copy">
            <span className="section-label section-label-dark">GardenSpa в действии</span>
            <h2>Как GardenSpa связывает <em>погоду и план ухода.</em></h2>
            <p>
              Короткий обзор актуальной версии: погодное окно для обработки,
              отдельные планы для дачи и теплицы, календарь работ и встроенный справочник.
            </p>
            <div className="video-chapters" aria-label="Содержание видео">
              {videoChapters.map((chapter) => (
                <div key={chapter.time}>
                  <span>{chapter.time}</span>
                  <p>{chapter.label}</p>
                </div>
              ))}
            </div>
            <p className="video-note">Без звука · запись реального интерфейса · 60 секунд</p>
          </div>
          <div className="video-stage">
            <span className="video-ring video-ring-one" aria-hidden="true" />
            <span className="video-ring video-ring-two" aria-hidden="true" />
            <div className="video-phone">
              <video
                className="guide-video"
                controls
                playsInline
                preload="metadata"
                poster="/screenshots/app-today-weather-v1-0-39.png"
                aria-label="Видеообзор актуальной версии приложения GardenSpa"
              >
                <source src="/videos/gardenspa-guide-v1-0-39.mp4" type="video/mp4" />
                Ваш браузер не поддерживает воспроизведение видео.
              </video>
            </div>
          </div>
        </div>
      </section>

      <section className="how" id="how">
        <div className="how-image" />
        <div className="shell how-grid">
          <div className="how-phone-wrap">
            <div className="how-orbit orbit-one" />
            <div className="how-orbit orbit-two" />
            <PhonePreview compact screen="calendar" />
          </div>
          <div className="how-copy">
            <span className="section-label section-label-dark">Просто начать</span>
            <h2>Настройте уход за садом <em>за три шага.</em></h2>
            <div className="steps">
              {steps.map((step) => (
                <div className="step" key={step.number}>
                  <span>{step.number}</span>
                  <div>
                    <h3>{step.title}</h3>
                    <p>{step.text}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      <section className="privacy section-light" id="privacy">
        <div className="shell privacy-panel">
          <div className="privacy-art" aria-hidden="true">
            <span className="privacy-ring ring-one" />
            <span className="privacy-ring ring-two" />
            <span className="privacy-core"><BrandMark /></span>
          </div>
          <div className="privacy-copy">
            <span className="section-label">Локальный садовый дневник</span>
            <h2>Основные данные остаются <em>на вашем телефоне.</em></h2>
            <p>
              Сады, растения, программы, запланированные процедуры и препараты
              хранятся локально. Интернет нужен для актуального прогноза Open‑Meteo
              и перехода по внешним ссылкам.
            </p>
            <div className="privacy-points">
              <span><i>✓</i> Локальное хранение</span>
              <span><i>✓</i> План доступен офлайн</span>
              <span><i>✓</i> Экспорт данных о саде</span>
            </div>
          </div>
        </div>
      </section>

      <section className="closing">
        <div className="closing-image" />
        <div className="closing-shade" />
        <div className="shell closing-content">
          <BrandMark />
          <span className="section-label section-label-dark">GardenSpa</span>
          <h2>Уход за растениями — по плану.<br /><em>Ваш сад — в своём ритме.</em></h2>
          <p>Садовый дневник и календарь ухода для тех, кто растит с любовью.</p>
          <div className="closing-download">
            <a
              href={RUSTORE_URL}
              target="_blank"
              rel="noopener noreferrer"
              aria-label="Открыть страницу GardenSpa в RuStore"
            >
              <img
                src="/rustore-gardenspa-qr.png"
                alt="QR-код GardenSpa в RuStore"
                width={630}
                height={630}
                loading="lazy"
              />
            </a>
            <div className="closing-download-copy">
              <span>Android · RuStore</span>
              <strong>Скачайте GardenSpa</strong>
              <small>Отсканируйте QR-код или откройте RuStore по кнопке</small>
              <a
                className="button button-primary"
                href={RUSTORE_URL}
                target="_blank"
                rel="noopener noreferrer"
              >
                Скачать в RuStore <ArrowIcon />
              </a>
            </div>
          </div>
        </div>
      </section>

      <footer>
        <div className="shell footer-inner">
          <a href="#top" className="brand footer-brand">
            <BrandMark />
            <span>Garden<span>Spa</span></span>
          </a>
          <p>Приложение для садоводов и ухода за растениями</p>
          <a href={RUSTORE_URL} target="_blank" rel="noopener noreferrer">RuStore · Android · 2026</a>
        </div>
      </footer>
    </main>
  );
}
