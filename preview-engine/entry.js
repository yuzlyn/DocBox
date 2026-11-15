(function () {
  'use strict';

  var MarkdownIt = require('markdown-it');
  var taskLists = require('markdown-it-task-lists');
  var footnote = require('markdown-it-footnote');
  var mark = require('markdown-it-mark');
  var sub = require('markdown-it-sub');
  var sup = require('markdown-it-sup');
  var attrs = require('markdown-it-attrs');
  var deflist = require('markdown-it-deflist');
  var hljs = require('highlight.js/lib/common');

  var cfg = {
    flavor: 'GITHUB',
    showToc: true,
    syntaxHighlight: true,
    headingAnchors: false,
    math: true
  };
  var lastMarkdown = '';

  function escapeHtml(s) {
    return String(s)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  function mathInlineRule(state, silent) {
    var src = state.src;
    var pos = state.pos;
    if (src.charCodeAt(pos) !== 36) return false;
    if (src.charCodeAt(pos + 1) === 36) return false;
    var end = pos + 1;
    while (end < state.posMax) {
      var c = src.charCodeAt(end);
      if (c === 10) return false;
      if (c === 36 && src.charCodeAt(end - 1) !== 92) break;
      end++;
    }
    if (end >= state.posMax || src.charCodeAt(end) !== 36) return false;
    if (end === pos + 1) return false;
    if (!silent) {
      var token = state.push('math_inline', 'span', 0);
      token.content = src.slice(pos + 1, end);
    }
    state.pos = end + 1;
    return true;
  }

  function mathBlockRule(state, startLine, endLine, silent) {
    var start = state.bMarks[startLine] + state.tShift[startLine];
    var max = state.eMarks[startLine];
    var first = state.src.slice(start, max).trim();
    if (first.slice(0, 2) !== '$$') return false;
    if (silent) return true;

    var singleLine = first.length > 2 && first.slice(-2) === '$$';
    var lines = [];
    var next = startLine;
    var closed = singleLine;

    if (singleLine) {
      lines.push(first.slice(2, -2));
    } else {
      if (first.length > 2) lines.push(first.slice(2));
      next = startLine + 1;
      while (next < endLine) {
        var s = state.bMarks[next] + state.tShift[next];
        var e = state.eMarks[next];
        var line = state.src.slice(s, e).trim();
        if (line.slice(-2) === '$$') {
          var tail = line.slice(0, -2).trim();
          if (tail) lines.push(tail);
          closed = true;
          break;
        }
        lines.push(state.src.slice(s, e));
        next++;
      }
    }
    if (!closed) return false;

    var token = state.push('math_block', 'div', 0);
    token.block = true;
    token.content = lines.join('\n').trim();
    token.map = [startLine, next + 1];
    state.line = next + 1;
    return true;
  }

  function mathPlugin(md) {
    md.inline.ruler.after('backticks', 'math_inline', mathInlineRule);
    md.block.ruler.before('fence', 'math_block', mathBlockRule, {
      alt: ['paragraph', 'reference', 'blockquote', 'list']
    });
    md.renderer.rules.math_inline = function (tokens, idx) {
      var content = tokens[idx].content;
      if (!cfg.math) {
        return '<code class="math-raw">' + escapeHtml('$' + content + '$') + '</code>';
      }
      return '<span class="math-inline">' + escapeHtml('$' + content + '$') + '</span>';
    };
    md.renderer.rules.math_block = function (tokens, idx) {
      var content = tokens[idx].content;
      if (!cfg.math) {
        return '<pre class="math-raw"><code>' + escapeHtml('$$\n' + content + '\n$$') + '</code></pre>';
      }
      return '<div class="math-block">' + escapeHtml('$$' + content + '$$') + '</div>';
    };
  }

  function highlightCode(str, lang) {
    if (!cfg.syntaxHighlight) return '';
    if (lang && hljs.getLanguage(lang)) {
      try {
        return '<pre class="hljs"><code class="language-' + escapeHtml(lang) + '">' +
          hljs.highlight(str, { language: lang, ignoreIllegals: true }).value +
          '</code></pre>';
      } catch (e) { /* fall through */ }
    }
    return '<pre class="hljs"><code>' + escapeHtml(str) + '</code></pre>';
  }

  var options = { html: true, linkify: true, breaks: true, highlight: highlightCode };
  var mdGithub = new MarkdownIt(options);
  var mdCommon = new MarkdownIt('commonmark', options);

  function install(md, full) {
    md.use(mathPlugin);
    if (full) {
      md.use(taskLists, { enabled: false, label: true });
      md.use(footnote);
      md.use(mark);
      md.use(sub);
      md.use(sup);
      md.use(attrs);
      md.use(deflist);
    }
  }
  install(mdGithub, true);
  install(mdCommon, false);

  function slug(text, index) {
    var s = String(text || '').trim().toLowerCase()
      .replace(/\s+/g, '-')
      .replace(/[^\p{L}\p{N}\-]/gu, '');
    return s || 'section-' + index;
  }

  // 允許內聯 HTML 之後，文件內容就能帶任意標籤。這裡只拿掉會執行、會外連、
  // 會改版面骨架的那幾類，其餘（b / i / u / mark / span style / table / kbd…）保留。
  var BLOCKED_TAGS = 'script,iframe,frame,frameset,object,embed,applet,link,meta,base,form';
  var URL_ATTRS = { href: 1, src: 1, action: 1, formaction: 1, 'xlink:href': 1 };
  var BAD_URL = /^\s*(javascript|vbscript|data:text\/html)/i;

  function sanitizeHtml(root) {
    root.querySelectorAll(BLOCKED_TAGS).forEach(function (node) {
      if (node.parentNode) node.parentNode.removeChild(node);
    });
    root.querySelectorAll('*').forEach(function (el) {
      var attrs = el.attributes;
      if (!attrs || !attrs.length) return;
      for (var i = attrs.length - 1; i >= 0; i--) {
        var name = attrs[i].name.toLowerCase();
        var value = attrs[i].value || '';
        if (name.indexOf('on') === 0 || name === 'srcdoc') {
          el.removeAttribute(attrs[i].name);
        } else if (URL_ATTRS[name] && BAD_URL.test(value)) {
          el.removeAttribute(attrs[i].name);
        }
      }
      if (el.tagName === 'STYLE' && /@import/i.test(el.textContent || '')) {
        el.textContent = (el.textContent || '').replace(/@import[^;]*;/gi, '');
      }
    });
  }

  function postProcess(root) {
    sanitizeHtml(root);
    root.querySelectorAll('table').forEach(function (table) {
      if (table.parentNode && table.parentNode.classList.contains('table-wrap')) return;
      var wrapper = document.createElement('div');
      wrapper.className = 'table-wrap';
      table.parentNode.insertBefore(wrapper, table);
      wrapper.appendChild(table);
    });

    var headings = [];
    root.querySelectorAll('h1,h2,h3,h4,h5,h6').forEach(function (heading, i) {
      var level = Number(heading.tagName.charAt(1));
      var id = slug(heading.textContent, i);
      var suffix = 2;
      while (root.querySelector('#' + CSS.escape(id))) {
        id = slug(heading.textContent, i) + '-' + suffix;
        suffix++;
      }
      heading.id = id;
      headings.push({ level: level, id: id, text: heading.textContent });
      if (cfg.headingAnchors) {
        var anchor = document.createElement('a');
        anchor.className = 'heading-anchor';
        anchor.href = '#' + id;
        anchor.textContent = '#';
        heading.appendChild(anchor);
      }
    });

    if (cfg.showToc && headings.length >= 2) {
      var nav = document.createElement('nav');
      nav.className = 'toc';
      var title = document.createElement('div');
      title.className = 'toc-title';
      title.textContent = '目錄';
      nav.appendChild(title);
      var list = document.createElement('ul');
      headings.forEach(function (h) {
        var li = document.createElement('li');
        li.className = 'toc-l' + Math.min(h.level, 3);
        var a = document.createElement('a');
        a.href = '#' + h.id;
        a.textContent = h.text;
        li.appendChild(a);
        list.appendChild(li);
      });
      nav.appendChild(list);
      root.insertBefore(nav, root.firstChild);
    }

    root.querySelectorAll('a[href]').forEach(function (a) {
      var href = a.getAttribute('href') || '';
      if (/^\s*(javascript|data|vbscript):/i.test(href)) {
        a.removeAttribute('href');
        return;
      }
      a.setAttribute('rel', 'noopener noreferrer');
      if (/^https?:\/\//i.test(href)) a.setAttribute('target', '_blank');
    });

    root.querySelectorAll('img').forEach(function (img) {
      img.setAttribute('loading', 'lazy');
      img.addEventListener('error', function () { img.classList.add('img-broken'); });
    });

    root.querySelectorAll('pre').forEach(function (pre) {
      var btn = document.createElement('button');
      btn.type = 'button';
      btn.className = 'copy-btn';
      btn.textContent = '複製';
      btn.addEventListener('click', function () {
        var code = pre.querySelector('code') || pre;
        if (window.AndroidClipboard && window.AndroidClipboard.copy) {
          window.AndroidClipboard.copy(code.textContent);
          btn.textContent = '已複製';
          setTimeout(function () { btn.textContent = '複製'; }, 1500);
        }
      });
      pre.appendChild(btn);
    });
  }

  function renderNow() {
    var root = document.getElementById('content');
    if (!root) return;
    var md = cfg.flavor === 'COMMONMARK' ? mdCommon : mdGithub;
    root.innerHTML = md.render(String(lastMarkdown || ''));
    postProcess(root);
    if (cfg.math && window.MathJax && window.MathJax.typesetPromise) {
      if (window.MathJax.typesetClear) window.MathJax.typesetClear([root]);
      window.MathJax.typesetPromise([root]).catch(function () {});
    }
  }

  window.applyTheme = function (css) {
    document.documentElement.style.cssText = css || '';
  };

  window.applyConfig = function (next) {
    if (!next) return;
    Object.keys(cfg).forEach(function (key) {
      if (Object.prototype.hasOwnProperty.call(next, key)) cfg[key] = next[key];
    });
  };

  window.updateMarkdown = function (text, nextCfg) {
    lastMarkdown = String(text || '');
    if (nextCfg) window.applyConfig(nextCfg);
    renderNow();
  };

  // Markdown 渲染不依赖 MathJax；公式在 MathJax 就绪后补一次排版
  function typesetWhenReady(attempt) {
    if (window.MathJax && window.MathJax.startup && window.MathJax.startup.promise) {
      window.MathJax.startup.promise.then(function () {
        if (lastMarkdown) renderNow();
      }, function () {});
    } else if (attempt < 40) {
      setTimeout(function () { typesetWhenReady(attempt + 1); }, 100);
    }
  }
  typesetWhenReady(0);
})();
