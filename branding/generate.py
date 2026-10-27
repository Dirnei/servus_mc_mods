import sys
out=sys.argv[1]
CREAM='#f3e9d2'; BRASS='#c9a24a'; BRASS_D='#8a6a26'
def frame(bg1,bg2,accent,body):
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0" stop-color="{bg1}"/><stop offset="1" stop-color="{bg2}"/>
    </linearGradient>
    <linearGradient id="brass" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="#e6c76f"/><stop offset="0.5" stop-color="{BRASS}"/><stop offset="1" stop-color="{BRASS_D}"/>
    </linearGradient>
  </defs>
  <rect x="8" y="8" width="496" height="496" rx="96" fill="url(#bg)"/>
  <rect x="30" y="30" width="452" height="452" rx="76" fill="none" stroke="url(#brass)" stroke-width="12"/>
  <g fill="url(#brass)">
    <circle cx="72" cy="72" r="9"/><circle cx="440" cy="72" r="9"/><circle cx="72" cy="440" r="9"/><circle cx="440" cy="440" r="9"/>
  </g>
{body.replace("ACCENT",accent).replace("CREAM",CREAM)}
</svg>
'''
foundation='''  <g stroke-linecap="butt">
    <line x1="256" y1="124" x2="256" y2="388" stroke="CREAM" stroke-width="64"/>
    <line x1="124" y1="256" x2="388" y2="256" stroke="CREAM" stroke-width="64"/>
    <line x1="256" y1="124" x2="256" y2="388" stroke="ACCENT" stroke-width="18"/>
    <line x1="124" y1="256" x2="388" y2="256" stroke="ACCENT" stroke-width="18"/>
  </g>
  <g fill="CREAM" stroke="ACCENT" stroke-width="10">
    <rect x="224" y="98" width="64" height="26" rx="6"/><rect x="224" y="388" width="64" height="26" rx="6"/>
    <rect x="98" y="224" width="26" height="64" rx="6"/><rect x="388" y="224" width="26" height="64" rx="6"/>
  </g>
  <circle cx="256" cy="256" r="70" fill="CREAM" stroke="ACCENT" stroke-width="14"/>
  <circle cx="256" cy="256" r="30" fill="ACCENT"/>'''
alchemy='''  <path d="M224 112 h64 v86 a112 112 0 1 1 -64 0 z" fill="CREAM"/>
  <path d="M160 300 a96 96 0 0 0 192 0 z" fill="ACCENT" transform="translate(0 6)"/>
  <path d="M224 112 h64 v86 a112 112 0 1 1 -64 0 z" fill="none" stroke="#3a2a52" stroke-width="10" stroke-linejoin="round"/>
  <rect x="210" y="98" width="92" height="22" rx="8" fill="CREAM" stroke="#3a2a52" stroke-width="10"/>
  <g fill="CREAM" stroke="#3a2a52" stroke-width="6">
    <circle cx="226" cy="356" r="14"/><circle cx="282" cy="340" r="10"/><circle cx="262" cy="380" r="7"/>
  </g>
  <path d="M256 246 l26 44 h-52 z" fill="none" stroke="#3a2a52" stroke-width="9" stroke-linejoin="round"/>'''
shuttles='''  <g stroke="ACCENT" stroke-width="12" stroke-linecap="round" opacity="0.9">
    <line x1="72" y1="214" x2="128" y2="214"/><line x1="88" y1="252" x2="128" y2="252"/><line x1="72" y1="290" x2="128" y2="290"/>
  </g>
  <rect x="150" y="166" width="262" height="160" rx="34" fill="CREAM"/>
  <rect x="150" y="166" width="262" height="160" rx="34" fill="none" stroke="#173b38" stroke-width="10"/>
  <rect x="182" y="196" width="118" height="58" rx="14" fill="ACCENT"/>
  <rect x="318" y="196" width="64" height="58" rx="14" fill="#173b38"/>
  <rect x="150" y="266" width="262" height="18" fill="ACCENT"/>
  <line x1="88" y1="392" x2="440" y2="392" stroke="CREAM" stroke-width="16" stroke-linecap="round"/>
  <g fill="#173b38" stroke="CREAM" stroke-width="12">
    <circle cx="214" cy="346" r="34"/><circle cx="348" cy="346" r="34"/>
  </g>
  <g fill="ACCENT"><circle cx="214" cy="346" r="10"/><circle cx="348" cy="346" r="10"/></g>'''
servus='''  <path d="M330 150 c-30 -34 -150 -40 -152 30 c-2 76 156 60 156 140 c0 74 -128 76 -168 30" fill="none" stroke="CREAM" stroke-width="52" stroke-linecap="round"/>
  <path d="M330 150 c-30 -34 -150 -40 -152 30 c-2 76 156 60 156 140 c0 74 -128 76 -168 30" fill="none" stroke="ACCENT" stroke-width="14" stroke-linecap="round"/>'''
delicacies='''  <g fill="none" stroke="CREAM" stroke-width="12" stroke-linecap="round" opacity="0.85">
    <path d="M206 176 c-18 -20 18 -36 0 -58 c-10 -12 -4 -22 4 -28"/>
    <path d="M256 168 c-18 -20 18 -36 0 -58 c-10 -12 -4 -22 4 -28"/>
    <path d="M306 176 c-18 -20 18 -36 0 -58 c-10 -12 -4 -22 4 -28"/>
  </g>
  <path d="M144 338 a112 112 0 0 1 224 0 z" fill="CREAM" stroke="#3a1a14" stroke-width="10" stroke-linejoin="round"/>
  <path d="M184 300 a76 76 0 0 1 52 -66" fill="none" stroke="ACCENT" stroke-width="14" stroke-linecap="round"/>
  <circle cx="256" cy="214" r="18" fill="ACCENT" stroke="#3a1a14" stroke-width="10"/>
  <rect x="104" y="336" width="304" height="28" rx="14" fill="CREAM" stroke="#3a1a14" stroke-width="10"/>
  <rect x="176" y="372" width="160" height="18" rx="9" fill="ACCENT"/>'''
brewery='''  <g transform="translate(-20 0)">
    <path d="M320 222 h30 a44 44 0 0 1 44 44 v54 a44 44 0 0 1 -44 44 h-30" fill="none" stroke="#2e1f0c" stroke-width="44"/>
    <path d="M320 222 h30 a44 44 0 0 1 44 44 v54 a44 44 0 0 1 -44 44 h-30" fill="none" stroke="CREAM" stroke-width="24"/>
    <rect x="150" y="176" width="176" height="234" rx="22" fill="ACCENT" stroke="#2e1f0c" stroke-width="10"/>
    <g stroke="CREAM" stroke-width="10" stroke-linecap="round" opacity="0.45">
      <line x1="192" y1="228" x2="192" y2="372"/><line x1="238" y1="228" x2="238" y2="372"/><line x1="284" y1="228" x2="284" y2="372"/>
    </g>
    <g fill="CREAM" opacity="0.8"><circle cx="214" cy="330" r="7"/><circle cx="262" cy="294" r="5"/><circle cx="300" cy="350" r="6"/></g>
    <g fill="CREAM" stroke="#2e1f0c" stroke-width="8">
      <circle cx="170" cy="176" r="30"/><circle cx="214" cy="160" r="38"/><circle cx="262" cy="166" r="34"/><circle cx="304" cy="174" r="30"/>
    </g>
    <g fill="CREAM"><circle cx="170" cy="176" r="26"/><circle cx="214" cy="160" r="34"/><circle cx="262" cy="166" r="30"/><circle cx="304" cy="174" r="26"/>
      <rect x="146" y="170" width="184" height="42"/><rect x="252" y="200" width="22" height="44" rx="11"/></g>
  </g>'''
for name,(b1,b2,acc,body) in {'foundation':('#2c3a4a','#1a222c','#d47a3a',foundation),'alchemy':('#3d2c55','#1f1630','#e8b23a',alchemy),'shuttles':('#1f4a46','#112624','#3cc4b0',shuttles),'delicacies':('#5a2a24','#2a1310','#e86a3a',delicacies),'brewery':('#4a3318','#22170a','#f2a93b',brewery),'servus':('#3a3530','#1e1b18','#c9a24a',servus)}.items():
    open(f'{out}/{name}.svg','w',newline='\n').write(frame(b1,b2,acc,body))
