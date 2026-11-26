/* URBO app: TV remote navigation. The D-pad moves a yellow focus ring to the nearest
   link or button in that direction, OK opens it, and the page scrolls when nothing is there. */
(function(){
  const st=document.createElement('style');
  st.textContent=':focus{outline:none}.tvf{outline:3px solid #f4c21c!important;outline-offset:3px;border-radius:6px;box-shadow:0 0 0 6px rgba(244,194,28,.25)!important}';
  document.head.appendChild(st);
  const SEL='a[href],button,input,select,summary,[tabindex]:not([tabindex="-1"]),label>input';
  let usingKeys=false;
  const vis=el=>{const r=el.getBoundingClientRect();if(!r.width||!r.height)return false;const cs=getComputedStyle(el);if(cs.visibility==='hidden'||cs.display==='none')return false;
    if(el.closest('[hidden],details:not([open]) > :not(summary)'))return false;return true};
  function all(){return[...document.querySelectorAll(SEL)].filter(vis)}
  function mark(el){document.querySelectorAll('.tvf').forEach(e=>e.classList.remove('tvf'));if(el){el.classList.add('tvf')}}
  function focusEl(el){if(!el)return;el.focus({preventScroll:true});el.scrollIntoView({block:'nearest',inline:'nearest',behavior:'smooth'});if(usingKeys)mark(el)}
  function move(dir){const cur=document.activeElement&&document.activeElement!==document.body?document.activeElement:null;const items=all();
    if(!cur||!items.includes(cur)){const top=items.find(e=>{const r=e.getBoundingClientRect();return r.top>=0&&r.top<innerHeight})||items[0];focusEl(top);return true}
    const a=cur.getBoundingClientRect(),ax=a.left+a.width/2,ay=a.top+a.height/2;let best=null,bs=1e12;
    items.forEach(el=>{if(el===cur)return;const b=el.getBoundingClientRect(),bx=b.left+b.width/2,by=b.top+b.height/2,dx=bx-ax,dy=by-ay;
      let main,cross;
      if(dir==='up'){if(b.bottom>a.top+2)return;main=a.top-b.bottom;cross=Math.abs(dx)}
      else if(dir==='down'){if(b.top<a.bottom-2)return;main=b.top-a.bottom;cross=Math.abs(dx)}
      else if(dir==='left'){if(b.right>a.left+2)return;main=a.left-b.right;cross=Math.abs(dy)}
      else{if(b.left<a.right-2)return;main=b.left-a.right;cross=Math.abs(dy)}
      const sc=Math.max(0,main)+cross*2.2;if(sc<bs){bs=sc;best=el}});
    if(best&&bs<innerHeight*1.6){focusEl(best);return true}
    if(dir==='up'||dir==='down'){scrollBy({top:(dir==='up'?-1:1)*innerHeight*.6,behavior:'smooth'});return true}
    return false}
  document.addEventListener('keydown',e=>{
    const k={ArrowUp:'up',ArrowDown:'down',ArrowLeft:'left',ArrowRight:'right'}[e.key];
    const t=document.activeElement;
    if(e.key==='Enter'&&t&&t.matches('summary,label,[tabindex]')&&!t.matches('a,button,input')){t.click();e.preventDefault();return}
    if(!k)return;usingKeys=true;
    if(t&&t.matches('input[type=search],input[type=text],textarea')&&(k==='left'||k==='right'))return;
    if(move(k))e.preventDefault()},true);
  document.addEventListener('pointerdown',()=>{usingKeys=false;mark(null)},true);
  document.addEventListener('focusin',e=>{if(usingKeys)mark(e.target)});
  // after each page change, put the focus ring on the main menu item
  window.addEventListener('hashchange',()=>setTimeout(()=>{if(!usingKeys)return;const c=document.querySelector('#nav [aria-current=page]')||all()[0];focusEl(c)},80));
  // make tiles, cards and the flag menu reachable
  new MutationObserver(()=>{document.querySelectorAll('.tile,.lvlbig').forEach(e=>{if(!e.matches(SEL))e.setAttribute('tabindex','0')})}).observe(document.documentElement,{subtree:true,childList:true});
})();
