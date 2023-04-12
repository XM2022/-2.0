toggle = document.querySelectorAll("#menu")[0];
nav = document.querySelectorAll("nav")[0];
toggle_open_text = '菜单';
toggle_close_text = '收起';

toggle.addEventListener('click', function() {
	nav.classList.toggle('open');
	
  if (nav.classList.contains('open')) {
    toggle.innerHTML = toggle_close_text; 
  } else {
    toggle.innerHTML = toggle_open_text;
  }
}, false);

shine = document.querySelector("#shine")
toggle.addEventListener('mouseover', function(){
    shine.style.filter = "blur(25px)"
}, false);
toggle.addEventListener('mouseleave', function(){
    shine.style.filter = "blur(0px)"
}, false);
