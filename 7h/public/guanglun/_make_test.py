import io, sys

src = "D:/workSpace/-/7h/public/guanglun/index.html"
dst = "D:/workSpace/-/7h/public/guanglun/_test.html"

html = open(src, encoding="utf-8").read()

probe = """
<script>
(function () {
  window.__err = [];
  window.addEventListener("error", function (e) { window.__err.push(String(e.message)); });
  window.addEventListener("unhandledrejection", function (e) { window.__err.push("rej:" + e.reason); });

  var params = new URLSearchParams(location.search);
  if (params.has("autotest")) {
    setTimeout(function () {
      var mode = params.get("mode") || "pve";
      var diff = params.get("diff") || "normal";
      var m = document.querySelector('#modeSeg button[data-mode="' + mode + '"]');
      if (m) m.click();
      var d = document.querySelector('#diffSeg button[data-diff="' + diff + '"]');
      if (d) d.click();
      var s = document.getElementById("btnStart");
      if (s) s.click();
    }, 120);
  }

  setTimeout(function () {
    var out = document.createElement("div");
    out.id = "__probe";
    var rn = document.getElementById("roundNo");
    var s1 = document.getElementById("s1");
    var s2 = document.getElementById("s2");
    var b1 = document.getElementById("b1");
    out.setAttribute("data-err", (window.__err.join(" | ") || "none"));
    out.setAttribute("data-round", rn ? rn.textContent : "?");
    out.setAttribute("data-score", (s1 ? s1.textContent : "?") + "-" + (s2 ? s2.textContent : "?"));
    out.setAttribute("data-buffs", b1 ? b1.textContent : "?");
    out.textContent = "probe";
    document.body.appendChild(out);
  }, 5000);
})();
</script>
</body>"""

assert html.count("</body>") == 1
html = html.replace("</body>", probe)
open(dst, "w", encoding="utf-8").write(html)
print("written", dst)
