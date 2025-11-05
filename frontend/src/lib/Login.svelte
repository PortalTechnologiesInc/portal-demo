
<script>

  import Theme from './Theme.svelte';
  import QRCode from '@castlenine/svelte-qrcode';
  import { connected , messages, errors} from '../socket.svelte.js';
  import { loggedIn, profile } from '../state.svelte.js';
  // listen messages and errors and check last message 
  let qrCodeUrl = '';

  $: if ($messages.length > 0) {
    let lastMessage = $messages[$messages.length - 1];
    if (lastMessage.cmd === 'KeyHandshakeUrlRequest') {
      qrCodeUrl = lastMessage.url;
    }

    if (lastMessage.cmd === 'AuthenticateKeyRequest') {
      loggedIn.set(true);
      profile.set(lastMessage.profile);
    }
  }

  let activeTab = 'qrCode';

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
    </ul>
  </div>
  {#if activeTab === 'qrCode'}
    <p class="text-muted-foreground">
      Scan this QR code with your Portal app to login from another device.
    </p>

    {#if qrCodeUrl}
    <div class="mt-6 flex justify-center">
        <QRCode data={qrCodeUrl} />
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
    <form class="flex items-center gap-2 mt-4">
      <input class="uk-input" type="text" placeholder="Enter static token..." aria-label="Input" />
      <button class="uk-btn uk-btn-default" type="submit">GO</button>
    </form>
    
  {/if}
  </div>
</div>
</div>
