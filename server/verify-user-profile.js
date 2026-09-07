'use strict';
const net = require('net');
const c = net.createConnection({ host: '127.0.0.1', port: 8899 }, () => {
  c.on('data', d => {
    const lines = d.toString('utf8').split('\n').filter(x => x.trim());
    for (const l of lines) {
      let o; try { o = JSON.parse(l); } catch (e) { continue; }
      if (o.type === 'login_ok') {
        c.write(JSON.stringify({ type: 'get_user_profile', username: 'cathe' }) + '\n');
      } else if (o.type === 'user_profile') {
        console.log('USER_PROFILE OK: ' + o.username + ' nick=' + o.nickname + ' keys=' + Object.keys(o.profile).join(','));
        c.destroy();
        process.exit(0);
      } else if (o.type === 'error') {
        console.log('ERROR: ' + o.message);
        c.destroy();
        process.exit(1);
      }
    }
  });
  c.write(JSON.stringify({ type: 'auth', token: '416f37013f68a234b12b47637b1e24c9' }) + '\n');
});
setTimeout(() => { console.log('TIMEOUT'); process.exit(2); }, 4000);
