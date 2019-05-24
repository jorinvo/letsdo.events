!(function () {
  var $ = document.querySelector.bind(document);
  var $$ = document.querySelectorAll.bind(document);
  var $title = $('title');

  var $input = $('#image-upload-input');
  if ($input) {
    var $message = $('#image-upload-message');
    var $image = $('#image-upload-preview');
    var $clear = $('#image-upload-clear');
    var $delete = $('#delete-image-input');

    $input.addEventListener('change', function() {
      var file = this.files[0];
      if(file.size > 5*1024*1024) {
        alert('Exceeded size 5MB');
        return;
      }
      $message.classList.add('hide');
      $image.setAttribute('src', URL.createObjectURL(file));
      if ($delete) {
        $delete.value = 'false';
      }
      $image.classList.remove('hide');
      $clear.classList.remove('hide');
    });

    $clear.addEventListener('click', function() {
      $input.value = '';
      $message.classList.remove('hide');
      $image.classList.add('hide');
      $clear.classList.add('hide');
      if ($delete) {
        $delete.value = 'true';
      }
    });
  }

  var $loginContainer = $('.login-container');
  if ($loginContainer) {
    var $loginForm = $loginContainer.querySelector('form');
    var $loginHeading = $('.login-heading');
    $loginHeading.addEventListener('click', function(e) {
      e.preventDefault();
      $loginContainer.classList.add('login');
      $loginContainer.classList.remove('signup');
      $title.innerHTML = 'Login';
      $loginForm.action = $loginHeading.href
      history.replaceState({}, 'Login', $loginHeading.href);
    });
    var $signupHeading = $('.signup-heading');
    $signupHeading.addEventListener('click', function(e) {
      e.preventDefault();
      $loginContainer.classList.add('signup');
      $loginContainer.classList.remove('login');
      $title.innerHTML = 'Signup';
      $loginForm.action = $signupHeading.href
      history.replaceState({}, 'Signup', $signupHeading.href);
    });
  }

  var $$confirm = $$('[data-confirm]');
  if ($$confirm) {
    function listener(e) {
      e.preventDefault();
      var answer = confirm(this.attributes.getNamedItem('data-confirm').textContent);
      if (answer) {
        this.removeEventListener('click', listener);
        this.click();
      }
    }
    Array.from($$confirm).forEach(function($el) {
      $el.addEventListener('click', listener);
    });
  }
})();
