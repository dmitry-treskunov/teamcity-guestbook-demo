import React from 'react'
import Header, {Logo} from '@jetbrains/ring-ui/components/header/header'
import jbLogo from '@jetbrains/logos/jetbrains/jetbrains.svg'

import './app.css'
import Guestbook from '../guestbook/guestbook'

export default () => (
  <>
    <Header className={'header'}>
      <a href="/">
        <Logo glyph={jbLogo} size={Logo.Size.Size48} />
      </a>
      <h2>{'Michael was here'}</h2>
    </Header>
    <div className="app-content">
      <Guestbook />
    </div>
  </>
)
