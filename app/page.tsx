const features = [
  {
    number: "01",
    title: "Сады и растения",
    text: "Соберите домашние цветы, теплицу и участок в понятные пространства. У каждого растения — своё место и свой ритм.",
    accent: "leaf",
  },
  {
    number: "02",
    title: "Календарь заботы",
    text: "Полив, подкормка, обработка или пересадка появляются в календаре именно тогда, когда нужны.",
    accent: "sun",
  },
  {
    number: "03",
    title: "Препараты под рукой",
    text: "Сохраните назначение и норму расхода, чтобы не искать упаковку в тот момент, когда пора действовать.",
    accent: "drop",
  },
];

const steps = [
  {
    number: "1",
    title: "Создайте свой сад",
    text: "Дом, дача, теплица или отдельная коллекция — организуйте растения так, как удобно вам.",
  },
  {
    number: "2",
    title: "Добавьте заботу",
    text: "Назначьте процедуру, препарат и удобный интервал повторения.",
  },
  {
    number: "3",
    title: "Следуйте ритму",
    text: "GardenSpa соберёт предстоящие дела в спокойный недельный календарь.",
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

function PhonePreview({ compact = false }: { compact?: boolean }) {
  return (
    <div className={compact ? "phone phone-compact" : "phone"}>
      <div className="phone-frame">
        <div className="phone-island" />
        <div className="phone-screen">
          <div className="phone-glow" />
          <div className="phone-status">
            <span>9:41</span>
            <span className="status-icons" aria-hidden="true">● ◒</span>
          </div>
          <div className="phone-brand">
            <BrandMark />
            <span>GardenSpa</span>
          </div>
          <div className="phone-greeting">
            <span className="eyebrow">Доброе утро</span>
            <strong>Марина</strong>
            <p>Ваш сад чувствует себя хорошо.</p>
          </div>
          <div className="phone-metrics">
            <div><strong>3</strong><span>сада</span></div>
            <div><strong>14</strong><span>растений</span></div>
            <div><strong>2</strong><span>сегодня</span></div>
          </div>
          <div className="phone-card">
            <div className="phone-card-head">
              <span>Сегодня</span>
              <small>30 июля</small>
            </div>
            <div className="task-row">
              <span className="task-dot task-dot-water" />
              <span><strong>Полить гортензию</strong><small>Сад у дома</small></span>
              <b>08:00</b>
            </div>
            <div className="task-row">
              <span className="task-dot task-dot-leaf" />
              <span><strong>Подкормить монстеру</strong><small>Домашние растения</small></span>
              <b>18:30</b>
            </div>
          </div>
          <div className="phone-next">
            <span>Ближайшие дни</span>
            <div className="mini-week">
              {["Пн", "Вт", "Ср", "Чт", "Пт"].map((day, index) => (
                <span className={index === 2 ? "active-day" : ""} key={day}>
                  <small>{day}</small>
                  <b>{28 + index}</b>
                </span>
              ))}
            </div>
          </div>
          <div className="phone-nav" aria-hidden="true">
            <span className="nav-active">⌂</span><span>⌁</span><span>＋</span><span>◫</span>
          </div>
        </div>
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
            <a href="#how">Как это работает</a>
            <a href="#privacy">Приватность</a>
          </div>
          <a href="#possibilities" className="nav-cta">
            Познакомиться <ArrowIcon />
          </a>
        </nav>

        <div className="hero-content shell">
          <div className="hero-copy">
            <div className="kicker"><span /> Персональный помощник садовода</div>
            <h1>Ваш сад.<br /><em>В своём ритме.</em></h1>
            <p>
              GardenSpa помогает вовремя поливать, подкармливать и заботиться
              о каждом растении — без суеты и лишних напоминаний.
            </p>
            <div className="hero-actions">
              <a className="button button-primary" href="#how">
                Как это работает <ArrowIcon />
              </a>
              <span className="platform-note">
                <span className="android-dot" />
                Приложение для Android
              </span>
            </div>
          </div>
          <div className="hero-phone">
            <div className="phone-aura" />
            <PhonePreview />
            <div className="floating-note floating-note-top">
              <span className="note-icon note-icon-leaf" />
              <span><small>Следующая забота</small><strong>Полив через 2 дня</strong></span>
            </div>
            <div className="floating-note floating-note-bottom">
              <span className="pulse-dot" />
              <span><small>Данные</small><strong>Только на вашем телефоне</strong></span>
            </div>
          </div>
        </div>

        <div className="hero-foot shell">
          <span>Сады</span><i />
          <span>Растения</span><i />
          <span>Календарь</span><i />
          <span>Препараты</span>
        </div>
      </section>

      <section className="intro section-light">
        <div className="shell intro-grid">
          <div>
            <span className="section-label">Забота без перегруза</span>
            <h2>Всё важное о растениях — <em>в одном спокойном месте.</em></h2>
          </div>
          <div className="intro-copy">
            <p>
              Когда растений становится больше, память превращается в бесконечный
              список: кого полить, чем обработать, что пересадить. GardenSpa
              бережно собирает эту заботу в понятную систему.
            </p>
            <div className="intro-facts">
              <span><b>01</b> Без облака</span>
              <span><b>02</b> Без рекламы</span>
              <span><b>03</b> Без лишнего шума</span>
            </div>
          </div>
        </div>
      </section>

      <section className="possibilities section-light" id="possibilities">
        <div className="shell">
          <div className="section-head">
            <span className="section-label">Возможности</span>
            <p>Три опоры для уверенного ухода</p>
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

      <section className="how" id="how">
        <div className="how-image" />
        <div className="shell how-grid">
          <div className="how-phone-wrap">
            <div className="how-orbit orbit-one" />
            <div className="how-orbit orbit-two" />
            <PhonePreview compact />
          </div>
          <div className="how-copy">
            <span className="section-label section-label-dark">Просто начать</span>
            <h2>От первого растения — к своему <em>ритму заботы.</em></h2>
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
            <span className="section-label">Личное остаётся личным</span>
            <h2>Ваш сад живёт <em>на вашем телефоне.</em></h2>
            <p>
              GardenSpa не требует облака или сервера. Сады, растения,
              процедуры и препараты хранятся локально — там, где им и место.
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
          <h2>Больше времени любоваться.<br /><em>Меньше — вспоминать.</em></h2>
          <p>Спокойный помощник для тех, кто растит с любовью.</p>
          <a className="button button-primary" href="#top">
            Вернуться в начало <span aria-hidden="true">↑</span>
          </a>
        </div>
      </section>

      <footer>
        <div className="shell footer-inner">
          <a href="#top" className="brand footer-brand">
            <BrandMark />
            <span>Garden<span>Spa</span></span>
          </a>
          <p>Персональный помощник садовода</p>
          <span>Android · 2026</span>
        </div>
      </footer>
    </main>
  );
}
