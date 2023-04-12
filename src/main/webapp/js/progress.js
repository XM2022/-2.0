function getProgress(option) {
    let progress = document.getElementById(option.id ? option.id : "progress");  
    let progressBar = document.getElementById(option.barId ? option.barId : "progress-bar");
    if(option.width){
         progress.style.width = option.width;
    } 
    if(option.height){
        progress.style.height = option.height;
        progressBar.style.height = option.height;
        progressBar.style.lineHeight = option.height;
        progressBar.style.fontSize = option.height;
    } 
    if(option.color) (progressBar.style.background = option.color);
    if(option.backgroundColor) (progress.style.background = option.backgroundColor);
    if(option.box) {
        progress.style.borderRadius = "0"
        progressBar.style.borderRadius = "0"
    }
    if(option.numberColor) progressBar.style.color = option.numberColor;
    
    return {
         value: progressBar,
         set(percentage) {
             if(percentage < 0)  return;
             if(percentage > 100) {
                 this.value.style.width = "100%"; 
                 this.value.innerHTML = "100%";
                 return;
             }
             this.value.style.width=`${percentage}%`; // 控制css进度条的进度
             this.value.innerHTML = `${percentage}%`; // 修改显示的进度值
         }
    }
}