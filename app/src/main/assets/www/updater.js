/* URBO app: the "Update app" button. Checks GitHub for a newer build when the app opens,
   shows a banner if there is one, and keeps a button in the footer to check any time. */
(function(){
  const N=window.UrboNative;if(!N)return;
  const st=document.createElement('style');
  st.textContent='.upd-bar{position:fixed;left:50%;bottom:16px;transform:translateX(-50%);z-index:60;display:flex;gap:12px;align-items:center;background:#1c1a16;color:#fff;padding:10px 12px 10px 18px;border-radius:12px;box-shadow:0 8px 24px #0006;font:600 15px "Fira Sans",sans-serif;max-width:calc(100vw - 24px)}.upd-bar button,.upd-btn{font:700 15px "Fira Sans",sans-serif;padding:9px 16px;border-radius:8px;border:0;background:#f4c21c;color:#1c1a16;cursor:pointer}.upd-bar .x{background:transparent;color:#bbb;padding:6px 8px}.upd-box{margin-top:10px}.upd-msg{display:block;font-size:13px;color:var(--mute,#777);margin-top:6px}';
  document.head.appendChild(st);
  let found=null,busy=false;
  const msg=t=>{const m=document.querySelector('.upd-msg');if(m)m.textContent=t;const b=document.querySelector('.upd-bar span');if(b&&busy)b.textContent=t};
  function bar(){let b=document.querySelector('.upd-bar');if(b)return b;b=document.createElement('div');b.className='upd-bar';b.setAttribute('role','status');
    b.innerHTML='<span></span><button class="go">Install</button><button class="x" aria-label="Close">✕</button>';document.body.appendChild(b);
    b.querySelector('.go').onclick=install;b.querySelector('.x').onclick=()=>b.remove();return b}
  function install(){if(!found||busy)return;busy=true;const b=bar();b.querySelector('.go').hidden=true;msg('Downloading the update…');N.install(found.url)}
  window.urboUpdate={
    found(latest,cur,url){found={latest,cur,url};const b=bar();b.querySelector('span').textContent='A new URBO update is ready (build '+latest+').';b.querySelector('.go').hidden=false;msg('Update available: build '+latest+'. You have build '+cur+'.');const fb=document.querySelector('.upd-btn');if(fb)fb.textContent='Install update'},
    fail(why){busy=false;msg('Couldn’t check for updates. '+(why||'')+' You can also install with Downloader.')},
    progress(p){msg('Downloading the update… '+p+'%')},
    state(s){const t={current:'You have the newest version (build '+N.appBuild()+').',offline:'Couldn’t reach GitHub. Check the internet connection and try again.',error:'The update didn’t work. Please try again.',permission:'Allow URBO to install apps, then go back. The update will continue.',installing:'Follow Android’s steps to finish installing.'}[s]||s;
      if(s!=='installing'&&s!=='permission')busy=false;msg(t);const b=document.querySelector('.upd-bar');if(b&&s==='current')b.remove()}};
  function footer(){const f=document.querySelector('footer .fgrid');if(!f||f.querySelector('.upd-box'))return;const d=document.createElement('div');d.className='upd-box';
    d.innerHTML='<b>URBO app</b><p>Build '+N.appBuild()+'</p><button class="upd-btn">Check for updates</button><span class="upd-msg"></span>';f.appendChild(d);
    d.querySelector('.upd-btn').onclick=()=>{if(found){install();return}msg('Checking…');N.check()}}
  const start=()=>{footer();setTimeout(()=>N.check(),2500)};
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',start);else start();
  new MutationObserver(footer).observe(document.body||document.documentElement,{childList:true,subtree:false});
})();
