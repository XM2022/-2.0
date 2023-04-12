//消息对话框
function showToast(msg,type){  //msg: 消息内容;  type:消息类型:支持success、error、warn三种.
    var messageEle = document.getElementsByClassName("parent-tips");
    //获取最后一个消息提示的显示层级，新的消息提示层级在此基础上+1
    var zIndex = messageEle.length == 0 ? 30 + 1 : parseInt(messageEle[messageEle.length-1].style.zIndex) + 1;
    var parent = document.createElement('div');
    parent.style.display = "block";
    parent.style.zIndex = zIndex;
    parent.className = "parent-tips";
    var tip = document.createElement('div') ;
    tip.className = "global-msg-tips " + type;  
    var span_ele = document.createElement('span');
    let span_width = span_ele.offsetWidth
    span_ele.textContent = msg;
    tip.appendChild(span_ele);
    parent .appendChild(tip);
    document.body.appendChild(parent);
    span_width = span_ele.offsetWidth - span_width
    tip.style.width = span_width+60+"px"
    setTimeout(function() {
        document.body.removeChild(parent);
    }, 2000)
 }