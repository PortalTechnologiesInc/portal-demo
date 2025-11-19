
<script>

  import Theme from './Theme.svelte';
  import QRCode from '@castlenine/svelte-qrcode';
  import { connected , messages, errors, sendWsMessage} from '../socket.svelte.js';
  import { loggedIn, profile, sessionToken, pubkey } from '../state.svelte.js';
  import { onMount } from 'svelte';

  let activeTab = 'qrCode';

  // listen messages and errors and check last message 
  let qrCodeUrl = '';

  let pendingAuthRequest = false;
  $: if ($messages.length > 0) {
    let lastMessage = $messages[$messages.length - 1];
    if (lastMessage.cmd === 'KeyHandshakeUrlRequest') {
      qrCodeUrl = lastMessage.url;
      console.log('qrCodeUrl', qrCodeUrl);
    }

    if (lastMessage.cmd === 'AuthenticateKeyRequest') {
      loggedIn.set(true);
      sessionToken.set(lastMessage.sessionToken);
      profile.set(lastMessage.state.profile);
      pubkey.set(lastMessage.state.key);
    }

    if (lastMessage.cmd === 'PendingAuthRequest') {
      pendingAuthRequest = true;
    }
    if (lastMessage.cmd === 'CancelledAuthRequest') {
      pendingAuthRequest = false;

      generateDynamicQrCode();
    }
  }

  onMount(() => {
    generateDynamicQrCode();
  })


  function generateDynamicQrCode() {
    sendWsMessage('GenerateQRCode,');
  }

  let staticToken = '';
  function generateStaticQrCode() {
    sendWsMessage('GenerateQRCode,' + staticToken);
  }



  let nip05Address = '';
  function loginWithNip05() {
    sendWsMessage('LoginWithNip05,' + nip05Address);
  }
</script>

<div
class="flex min-h-svh items-center justify-center p-4 md:bg-muted md:p-10"
>
<div class="w-full max-w-md">
  <div class="mb-6 flex justify-center">
    <a href="#">
      <img
        src="portal-logo-normal.webp"
        alt="Portal logo."
        width="75"
      />
    </a>
  </div>
  <div
    class="fr-widget border-border bg-background text-foreground md:border md:p-6 text-center"
  >

  <div class="flex flex-col space-y-3.5">
    <h1 class="uk-h4">Login</h1>
    <p class="text-muted-foreground">
     Choose a login method to get started.
    </p>
  </div>

  <div class="mt-4 mb-4">
    <ul class="justify-center" data-uk-tab>
      <li class="uk-active"><a href="#" on:click={() => activeTab = 'qrCode'}>QR Code</a></li>
      <li><a href="#" on:click={() => activeTab = 'hardLink'}>Hard Link</a></li>
      <li><a href="#" on:click={() => activeTab = 'staticQrCode'}>Static QR Code</a></li>
      <li><a href="#" on:click={() => activeTab = 'nip05'}>Nip05</a></li>

    </ul>
  </div>
  {#if activeTab === 'qrCode'}
    <p class="text-muted-foreground">
      Scan this QR code with your Portal app to login from another device.
    </p>

    {#if qrCodeUrl && !pendingAuthRequest}
    <div class="mt-6 flex justify-center">
        {#key qrCodeUrl}
          <QRCode data={qrCodeUrl} />
        {/key}
      </div>
    {/if}
    {#if pendingAuthRequest}
    <div class="mt-6 flex justify-center">
      <div data-uk-spinner></div>
    </div>
    {/if}
  {/if}


  {#if activeTab === 'hardLink'}
    <div class="flex flex-col space-y-2.5">
      <p class="text-muted-foreground">
       Or click below to login from this device.
      </p>
        <a class="uk-btn uk-btn-default" href={qrCodeUrl} >
          Login with Portal
        </a>
    </div>
  {/if}

  {#if activeTab === 'staticQrCode'}
    <p class="text-muted-foreground">
      Enter a static token to generate a custom QR code 
    </p>

    <!-- set button on right side of input with a gap of 10px -->
    <div class="flex items-center gap-2 mt-4">
      <input disabled={pendingAuthRequest} class="uk-input" type="text" placeholder="Enter static token..." aria-label="Input" bind:value={staticToken} />
      {#if pendingAuthRequest}
      <div data-uk-spinner></div>
      {:else}
      <button class="uk-btn uk-btn-default" type="submit" on:click={generateStaticQrCode}>GO</button>
      {/if}
    </div>

    {#if qrCodeUrl}
      <div class="mt-6 flex justify-center">
        {#key qrCodeUrl}
          <QRCode data={qrCodeUrl} />
        {/key}
      </div>
    {/if}
    
  {/if}

  {#if activeTab === 'nip05'}
    <p class="text-muted-foreground">
      Enter a Nip05 address to login
    </p>

    <div class="flex items-center gap-2 mt-4">
      <input disabled={pendingAuthRequest} class="uk-input" type="text" placeholder="username@getportal.cc" aria-label="Input" bind:value={nip05Address} />
      {#if pendingAuthRequest}
      <div data-uk-spinner></div>
      {:else}
        <button class="uk-btn uk-btn-default" type="submit" on:click={loginWithNip05} >GO</button>
      {/if}
    </div>

  {/if}
  </div>
</div>
</div>
