 /**可以生成信息提示对话框(一个确认按钮)、选择对话框(确认/取消按钮) */ 
 var ModelBox=(function(){
    var ModelBox=function(option){
        option.confirmCallBack = option.confirmCallBack ? option.confirmCallBack : ()=>{};
        option.cancelCallBack = option.cancelCallBack ? option.cancelCallBack : ()=>{};
      this.option=option||{};
      this.init();
    };
    ModelBox.prototype={
      isShow:false,
      id: 0,
      init:function(){
        var _this=this;
        _this.isShow=false;
        _this.id=_this.option.id;
        var html =
          '<div class="model-container">'
          +(_this.option.iconSrc?'<img  class="model-icon"/>':'')
          +(_this.option.iconSrc?'<div  class="model-blank"></div>':'')
          +'<h1 class="model-title">title</h1>'
          +'<div class="model-content c'+_this.id+'"></div>'
          +'<div class="controls">'
          +'<button class="confirm model-confirm">确定</button>'
          +'&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp'
          +'<button class="cancel model-cancel">取消</button>'
          +'</div>'
          +'</div>';
        var ModelBoxCon=document.createElement("div");
        ModelBoxCon.setAttribute("class","mask-layer c"+_this.id);
        ModelBoxCon.innerHTML=html;
        ModelBoxCon.querySelector(".model-title").innerHTML=_this.option.title;
        ModelBoxCon.querySelector(".model-confirm").innerHTML=_this.option.confirm ? _this.option.confirm : "";
        ModelBoxCon.querySelector(".model-cancel").innerHTML=_this.option.cancel;
        _this.option.iconSrc && (ModelBoxCon.querySelector(".model-icon").setAttribute("src",_this.option.iconSrc));
        !_this.option.isShowCancel && (ModelBoxCon.querySelector(".model-cancel").style.display = 'none');
        document.getElementsByTagName("body")[0].appendChild(ModelBoxCon);
        if(!_this.isShow){
          ModelBoxCon.style.display="none";
        }
        ModelBoxCon.querySelector(".cancel").onclick=ModelBoxCon.querySelector(".confirm").onclick=_this.eventsFn.bind("",this,ModelBoxCon);
        if(!_this.option.isShowCancel) {  //单按钮显示居中
            let cancelBtn = ModelBoxCon.querySelector(".model-confirm");
            cancelBtn.style.position = "relavtive";
            cancelBtn.style.transform = "translate(20%, 0)";
        }
      },
      setCallBack:function(confirmCallBack, cancelCallBack){
        this.option.confirmCallBack = confirmCallBack;
        this.option.cancelCallBack = cancelCallBack ? cancelCallBack : ()=>{};
      },
      show:function(content){
        document.querySelector(".model-content.c"+this.id).innerHTML=content;
        document.querySelector(".mask-layer.c"+this.id).style.display="block";
        this.isShow=true;
      },
      hide:function(){
        document.querySelector(".mask-layer").style.display="none";
        this.isShow=false;
      },
      eventsFn:function(e,doc,target){
        var _thisEvent=target.target;
        if(_thisEvent.classList.contains("confirm")){
          e.option.confirmCallBack();
        }
        if(_thisEvent.classList.contains("cancel")){
          e.option.cancelCallBack();
        }
        doc.style.display="none";
        e.isShow=false;
        return false;
      }
    }||{};
    return ModelBox;
  })();