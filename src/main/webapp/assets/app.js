/* Top-bar search behaviour and live suggestions.
   Progressive enhancement: without this script the search icon still toggles
   the box (it is a <details> element) and the form submits normally. With it:
   - clicking the icon opens the box and focuses it; clicking again submits;
   - the box collapses back to the icon when focus leaves it;
   - typing fetches the best-ranked papers (title matches first, then
     abstract) into a listbox, and arrow keys + Enter open a paper's page. */
(function () {
  "use strict";

  var panel = document.querySelector(".search-panel");
  if (!panel) {
    return;
  }
  var popup = panel.closest("details");
  var form = panel.closest("form");
  var toggle = popup ? popup.querySelector("summary") : null;
  var input = panel.querySelector('input[name="q"]');
  var list = panel.querySelector(".suggestions");
  var endpoint = panel.getAttribute("data-suggest");
  if (!popup || !form || !toggle || !input || !list) {
    return;
  }

  var MIN_LENGTH = 2;
  var DEBOUNCE_MS = 150;
  var timer = null;
  var latest = 0;
  var items = [];
  var active = -1;

  function closeList() {
    list.hidden = true;
    list.innerHTML = "";
    items = [];
    active = -1;
    input.setAttribute("aria-expanded", "false");
    input.removeAttribute("aria-activedescendant");
  }

  function collapse() {
    closeList();
    popup.open = false;
  }

  /* --- expand / collapse --- */

  toggle.addEventListener("click", function (event) {
    if (popup.open) {
      event.preventDefault();
      if (input.value.trim()) {
        form.submit();
      } else {
        collapse();
      }
    }
  });

  popup.addEventListener("toggle", function () {
    if (popup.open) {
      input.focus();
      input.select();
    } else {
      closeList();
    }
  });

  input.addEventListener("blur", function () {
    // Give a click on a suggestion or on the icon time to land first.
    window.setTimeout(function () {
      if (!popup.contains(document.activeElement)) {
        collapse();
      }
    }, 150);
  });

  /* --- suggestions --- */

  function setActive(index) {
    if (items.length === 0) {
      return;
    }
    if (index < 0) {
      index = items.length - 1;
    } else if (index >= items.length) {
      index = 0;
    }
    if (active >= 0 && items[active]) {
      items[active].classList.remove("is-active");
      items[active].setAttribute("aria-selected", "false");
    }
    active = index;
    var item = items[active];
    item.classList.add("is-active");
    item.setAttribute("aria-selected", "true");
    input.setAttribute("aria-activedescendant", item.id);
    if (typeof item.scrollIntoView === "function") {
      item.scrollIntoView({ block: "nearest" });
    }
  }

  function render(results) {
    closeList();
    if (!results.length) {
      return;
    }
    results.forEach(function (paper, index) {
      var item = document.createElement("li");
      item.id = "search-suggestion-" + index;
      item.setAttribute("role", "option");
      item.setAttribute("aria-selected", "false");
      item.className = "suggestion";

      var link = document.createElement("a");
      link.href = paper.url;
      link.tabIndex = -1;

      var title = document.createElement("span");
      title.className = "suggestion-title";
      title.textContent = paper.title;

      var meta = document.createElement("span");
      meta.className = "suggestion-meta";
      meta.textContent = paper.venue + " " + paper.year + (paper.authors ? " · " + paper.authors : "");

      link.appendChild(title);
      link.appendChild(meta);
      item.appendChild(link);
      item.addEventListener("mousedown", function (event) {
        // Navigate before the input's blur handler collapses the box.
        event.preventDefault();
        window.location.href = paper.url;
      });
      list.appendChild(item);
      items.push(item);
    });
    list.hidden = false;
    input.setAttribute("aria-expanded", "true");
  }

  function search() {
    var query = input.value.trim();
    if (!endpoint || typeof window.fetch !== "function" || query.length < MIN_LENGTH) {
      closeList();
      return;
    }
    var request = ++latest;
    fetch(endpoint + "?q=" + encodeURIComponent(query), { headers: { Accept: "application/json" } })
      .then(function (response) {
        return response.ok ? response.json() : [];
      })
      .then(function (results) {
        if (request === latest && document.activeElement === input) {
          render(Array.isArray(results) ? results : []);
        }
      })
      .catch(closeList);
  }

  input.addEventListener("input", function () {
    window.clearTimeout(timer);
    timer = window.setTimeout(search, DEBOUNCE_MS);
  });

  input.addEventListener("keydown", function (event) {
    if (event.key === "Escape") {
      event.preventDefault();
      collapse();
      toggle.focus();
      return;
    }
    if (list.hidden) {
      return;
    }
    switch (event.key) {
      case "ArrowDown":
        event.preventDefault();
        setActive(active + 1);
        break;
      case "ArrowUp":
        event.preventDefault();
        setActive(active - 1);
        break;
      case "Enter":
        if (active >= 0 && items[active]) {
          event.preventDefault();
          window.location.href = items[active].querySelector("a").href;
        }
        break;
      default:
        break;
    }
  });
})();
