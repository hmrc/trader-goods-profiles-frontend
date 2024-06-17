// prevent resubmit warning
if (window.history && window.history.replaceState && typeof window.history.replaceState === 'function') {
  window.history.replaceState(null, null, window.location.href);
}

document.addEventListener('DOMContentLoaded', function(event) {

  // handle back click
  var backLink = document.querySelector('.govuk-back-link');
  if (backLink !== null) {
    backLink.addEventListener('click', function(e){
      e.preventDefault();
      e.stopPropagation();
      window.history.back();
    });
  }
});

var loadButton = document.getElementById('load-button');
if (loadButton) {
  loadButton.addEventListener('click', function (e) {
    e.preventDefault();
    document.getElementById("spinning-wheel").hidden = false;
    loadButton.style.display = 'none';
    setTimeout(function () {
      document.getElementById("previousMovementForm").submit();
    }, 12000);
  });
}

