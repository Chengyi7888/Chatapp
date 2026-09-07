'use strict';
const net = require('net');
const c = net.createConnection({ host: '127.0.0.1', port: 8899 }, () => {
  let done = false;
  c.on('data', d => {
    if (done) return;
    const lines = d.toString('utf8').split('\n').filter(x => x.trim());
    for (const l of lines) {
      try {
        const o = JSON.parse(l);
        if (o.type === 'login_ok') {
          console.log('LOGIN OK: ' + o.username + ' nick=' + o.nickname);
          done = true;
          c.destroy();
          process.exit(0);
        } else if (o.type === 'error') {
          console.log('ERROR: ' + o.message);
          done = true;
          c.destroy();
          process.exit(1);
        } else if (o.type === 'contacts') {
          console.log('CONTACTS: ' + JSON.stringify(o.contacts.map(x => x.username)));
        } else if (o.type === 'conversations') {
          console.log('CONVS: ' + JSON.stringify(o.conversations.map(x => x.with + ':' + (x.lastText || ''))));
        } else if (o.type === 'groups') {
          console.log('GROUPS: ' + JSON.stringify(o.groups.map(x => x.name)));
        }
      } catch (e) {}
    }
  });
  c.write(JSON.stringify({ type: 'login', username: '澄意', password: '0522' }) + '\n');
});
setTimeout(() => { console.log('TIMEOUT'); process.exit(1); }, 4000);
