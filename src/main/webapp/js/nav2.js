toggle = document.querySelectorAll(".toggle")[0];
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

home_menu = document.querySelectorAll("#home-menu")[0];
home_menu.addEventListener('click', ()=>{
    if(nav.classList.contains('open')) {
        home_menu.style.left = "155px";
        home_menu.style.top = "145px";
    }
    else {
        home_menu.style.left = "120px";
        home_menu.style.top = "190px";
    }
})