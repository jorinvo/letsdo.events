!(function () {
  var $ = document.querySelector.bind(document);

  var $input = $('#image-upload-input');
  var $message = $('#image-upload-message');
  var $image = $('#image-upload-preview');
  var $clear = $('#image-upload-clear');
  var $delete = $('#delete-image-input');

  if ($input) {
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
})();
