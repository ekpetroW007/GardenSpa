const RUSTORE_URL = "https://www.rustore.ru/catalog/app/ru.samates.gardenspa";

const features = [
  {
    number: "01",
    title: "Садовый дневник",
    text: "Добавляйте домашние цветы, растения на даче и в теплице. У каждого сада и растения будет своя понятная карточка.",
    accent: "leaf",
  },
  {
    number: "02",
    title: "Календарь ухода",
    text: "Планируйте полив, подкормку, обработку и пересадку. GardenSpa вовремя напомнит о предстоящих садовых работах.",
    accent: "sun",
  },
  {
    number: "03",
    title: "Учёт препаратов",
    text: "Сохраняйте назначение и норму расхода препаратов, связывайте их с растениями и запланированными обработками.",
    accent: "drop",
  },
];

const steps = [
  {
    number: "1",
    title: "Создайте садовый дневник",
    text: "Добавьте дом, дачу, теплицу или отдельную коллекцию, а затем сохраните нужные растения.",
  },
  {
    number: "2",
    title: "Запланируйте уход",
    text: "Выберите полив, подкормку, пересадку или обработку, препарат и удобный интервал повторения.",
  },
  {
    number: "3",
    title: "Получайте напоминания",
    text: "GardenSpa соберёт садовые работы в календаре и напомнит, когда растению потребуется уход.",
  },
];

const videoChapters = [
  { time: "00:00", label: "Открытие календаря" },
  { time: "00:07", label: "Новое растение «Смородина»" },
  { time: "00:18", label: "Действие и препарат «Сера»" },
  { time: "00:25", label: "3 повтора через 5 дней" },
  { time: "00:39", label: "Сохранение и уведомление" },
  { time: "00:51", label: "Карточка и отметка «Выполнено»" },
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
  screen = "home",
}: {
  compact?: boolean;
  screen?: "home" | "calendar";
}) {
  const isCalendar = screen === "calendar";

  return (
    <div className={compact ? "phone phone-compact" : "phone"}>
      <div className="phone-frame phone-frame-real">
        <img
          className="phone-screenshot"
          src={
            isCalendar
              ? "/screenshots/app-calendar.png"
              : "/screenshots/app-home.png"
          }
          alt={
            isCalendar
              ? "Календарь ухода за растениями в приложении GardenSpa"
              : "Садовый дневник и растения в приложении GardenSpa"
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
            <a href="#video">Видео</a>
            <a href="#how">Как работает</a>
            <a href="#privacy">Приватность</a>
          </div>
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
              GardenSpa объединяет садовый дневник, календарь полива, подкормок
              и обработок. Планируйте работы и получайте напоминания — без суеты.
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
            <div className="hero-qr">
              <a
                href={RUSTORE_URL}
                target="_blank"
                rel="noopener noreferrer"
                aria-label="Открыть страницу GardenSpa в RuStore"
              >
                <img
                  src="/rustore-gardenspa-qr.png"
                  alt="QR-код для скачивания GardenSpa из RuStore"
                  width={630}
                  height={630}
                />
              </a>
              <span>
                <small>Для Android</small>
                <strong>Наведите камеру и скачайте GardenSpa</strong>
              </span>
            </div>
          </div>
          <div className="hero-phone">
            <div className="phone-aura" />
            <PhonePreview screen="home" />
            <div className="floating-note floating-note-top">
              <span className="note-icon note-icon-leaf" />
              <span><small>Статус на сегодня</small><strong>Всё идёт по плану</strong></span>
            </div>
            <div className="floating-note floating-note-bottom">
              <span className="pulse-dot" />
              <span><small>Данные</small><strong>Только на вашем телефоне</strong></span>
            </div>
          </div>
        </div>

        <div className="hero-foot shell">
          <span>Садовый дневник</span><i />
          <span>Календарь ухода</span><i />
          <span>Напоминания</span><i />
          <span>Учёт препаратов</span>
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
              поливать, чем подкармливать и обрабатывать. GardenSpa превращает
              разрозненные дела в понятный план садовых работ.
            </p>
            <div className="intro-facts">
              <span><b>01</b> Работает без интернета</span>
              <span><b>02</b> Без рекламы</span>
              <span><b>03</b> Данные на телефоне</span>
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

      <section className="video-guide" id="video">
        <div className="shell video-guide-grid">
          <div className="video-guide-copy">
            <span className="section-label section-label-dark">GardenSpa в действии</span>
            <h2>Как работает <em>календарь садовых работ.</em></h2>
            <p>
              Реальный сценарий ухода за растением: добавляем для смородины
              обработку серой, настраиваем три повтора через пять дней, получаем
              напоминание и отмечаем садовую работу выполненной.
            </p>
            <div className="video-chapters" aria-label="Содержание видео">
              {videoChapters.map((chapter) => (
                <div key={chapter.time}>
                  <span>{chapter.time}</span>
                  <p>{chapter.label}</p>
                </div>
              ))}
            </div>
            <p className="video-note">Без звука · каждое касание выделено кругом · 74 секунды</p>
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
                poster="/screenshots/app-home.png"
                aria-label="Видеоинструкция по приложению GardenSpa"
              >
                <source src="/videos/gardenspa-guide.mp4" type="video/mp4" />
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
            <span className="section-label">Офлайн-приложение для садовода</span>
            <h2>Данные о растениях остаются <em>на вашем телефоне.</em></h2>
            <p>
              GardenSpa работает без интернета, облака и сервера. Садовый
              дневник, растения, запланированные процедуры и препараты
              хранятся локально на устройстве.
            </p>
            <div className="privacy-points">
              <span><i>✓</i> Локальное хранение</span>
              <span><i>✓</i> Работа без интернета</span>
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
