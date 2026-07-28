(() => {
  const screens = [...document.querySelectorAll('[data-screen]')];
  const flowButtons = [...document.querySelectorAll('[data-jump]')];
  const historyStack = ['welcome'];
  let currentScreen = 'welcome';
  let toastTimer;

  const navItems = [
    { id: 'home', icon: '⌂', label: 'Главная' },
    { id: 'gardens', icon: '♧', label: 'Сады' },
    { id: 'calendar', icon: '◫', label: 'Календарь' },
    { id: 'drugs', icon: '⚗', label: 'Средства' }
  ];

  function renderBottomNavigation() {
    document.querySelectorAll('.bottom-nav').forEach(nav => {
      const active = nav.dataset.active;
      nav.innerHTML = navItems.map(item => `
        <button class="nav-item ${item.id === active ? 'is-active' : ''}" data-route="${item.id}" aria-label="${item.label}">
          <i aria-hidden="true">${item.icon}</i><span>${item.label}</span>
        </button>
      `).join('');
    });
  }

  function showScreen(name, push = true) {
    const target = screens.find(screen => screen.dataset.screen === name);
    if (!target || name === currentScreen) return;
    screens.forEach(screen => screen.classList.toggle('is-active', screen === target));
    if (push) historyStack.push(name);
    currentScreen = name;
    target.querySelector('.screen-scroll')?.scrollTo({ top: 0 });
    flowButtons.forEach(button => button.classList.toggle('is-current', button.dataset.jump === name));
    document.title = `Bookeeper — ${screenTitle(name)}`;
  }

  function screenTitle(name) {
    return ({
      welcome: 'Welcome', home: 'Главная', gardens: 'Мои сады',
      'garden-detail': 'Сад у дома', calendar: 'Календарь',
      'plant-detail': 'Яблоня Антоновка', drugs: 'Препараты',
      'drug-detail': 'Фитоверм', 'add-plant': 'Новое растение'
    })[name] || 'Glass Garden';
  }

  function goBack() {
    if (historyStack.length > 1) {
      historyStack.pop();
      showScreen(historyStack[historyStack.length - 1], false);
    } else if (currentScreen !== 'home') {
      showScreen('home');
    }
  }

  function toast(message) {
    const node = document.getElementById('toast');
    node.textContent = message;
    node.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => node.classList.remove('show'), 2200);
  }

  document.addEventListener('click', event => {
    const route = event.target.closest('[data-route]');
    if (route) {
      event.preventDefault();
      showScreen(route.dataset.route);
      return;
    }
    const jump = event.target.closest('[data-jump]');
    if (jump) {
      event.preventDefault();
      showScreen(jump.dataset.jump);
      return;
    }
    if (event.target.closest('[data-back]')) {
      event.preventDefault();
      goBack();
      return;
    }
    const filter = event.target.closest('.filter-chip');
    if (filter) {
      filter.parentElement.querySelectorAll('.filter-chip').forEach(chip => chip.classList.remove('is-selected'));
      filter.classList.add('is-selected');
      toast(`Фильтр «${filter.textContent.trim()}» применён`);
    }
  });

  document.addEventListener('keydown', event => {
    if (event.key === 'Escape' || (event.altKey && event.key === 'ArrowLeft')) goBack();
  });

  function bindSearch(inputId, listId, itemSelector) {
    const input = document.getElementById(inputId);
    const items = [...document.querySelectorAll(`#${listId} ${itemSelector}`)];
    input?.addEventListener('input', () => {
      const query = input.value.trim().toLocaleLowerCase('ru');
      items.forEach(item => { item.hidden = !item.dataset.name.includes(query); });
    });
  }

  function createCalendar() {
    const grid = document.getElementById('calendarGrid');
    const selectedTitle = document.getElementById('selectedDateTitle');
    const taskDays = new Set([3, 9, 15, 21, 27]);
    const days = [29, 30, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 1, 2, 3, 4, 5, 6, 7, 8, 9];
    grid.innerHTML = days.map((day, index) => {
      const other = index < 2 || index > 32;
      return `<button type="button" class="${other ? 'other ' : ''}${!other && taskDays.has(day) ? 'has-task ' : ''}${!other && day === 27 ? 'selected' : ''}" data-day="${day}" data-other="${other}">${day}</button>`;
    }).join('');
    grid.addEventListener('click', event => {
      const day = event.target.closest('[data-day]');
      if (!day || day.dataset.other === 'true') return;
      grid.querySelectorAll('button').forEach(button => button.classList.remove('selected'));
      day.classList.add('selected');
      selectedTitle.textContent = `${day.dataset.day} июля`;
    });
  }

  function setupActions() {
    document.getElementById('exportGarden')?.addEventListener('click', () => toast('Данные сада подготовлены к экспорту'));

    const favorite = document.getElementById('favoritePlant');
    favorite?.addEventListener('click', () => {
      favorite.classList.toggle('is-favorite');
      favorite.textContent = favorite.classList.contains('is-favorite') ? '♥' : '♡';
      toast(favorite.classList.contains('is-favorite') ? 'Растение добавлено в избранное' : 'Растение удалено из избранного');
    });

    const treatment = document.getElementById('treatmentCard');
    document.getElementById('completeTask')?.addEventListener('click', event => {
      treatment.classList.toggle('is-complete');
      event.currentTarget.textContent = treatment.classList.contains('is-complete') ? 'Готово ✓' : 'Выполнено';
      toast(treatment.classList.contains('is-complete') ? 'Задача отмечена выполненной' : 'Статус задачи восстановлен');
    });
    document.getElementById('rescheduleTask')?.addEventListener('click', () => {
      document.getElementById('selectedDateTitle').textContent = '28 июля';
      toast('Обработка перенесена на 28 июля');
    });

    document.getElementById('deleteDrug')?.addEventListener('click', () => toast('В прототипе удаление не изменяет данные'));

    document.getElementById('plantForm')?.addEventListener('submit', event => {
      event.preventDefault();
      const form = event.currentTarget;
      if (!form.reportValidity()) return;
      toast('Растение сохранено · напоминание создано');
      setTimeout(() => showScreen('garden-detail'), 650);
    });
  }

  let monthOffset = 0;
  const monthNames = ['Июль 2026', 'Август 2026', 'Сентябрь 2026'];
  function shiftMonth(direction) {
    monthOffset = Math.max(0, Math.min(monthNames.length - 1, monthOffset + direction));
    document.getElementById('monthTitle').textContent = monthNames[monthOffset];
    toast(monthNames[monthOffset]);
  }

  renderBottomNavigation();
  bindSearch('gardenSearch', 'gardenList', '.garden-card');
  bindSearch('drugSearch', 'drugList', '.drug-card');
  createCalendar();
  setupActions();
  document.getElementById('prevMonth')?.addEventListener('click', () => shiftMonth(-1));
  document.getElementById('nextMonth')?.addEventListener('click', () => shiftMonth(1));
  flowButtons.find(button => button.dataset.jump === 'welcome')?.classList.add('is-current');
})();
