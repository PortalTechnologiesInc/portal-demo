
<script>

  import Theme from './Theme.svelte';
  import QRCode from '@castlenine/svelte-qrcode';
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
    <div class="flex flex-col space-y-12">
      <h1 class="uk-h4">Login</h1>
      <p class="text-muted-foreground">
       Scan this QR code with your Portal app to login from another device.
      </p>
    </div>

    <div class="mt-6 flex justify-center">
      <QRCode data="Hello world!" />
    </div>

    <hr class="uk-hr mt-8 mb-8" />

    <div class="flex flex-col space-y-2.5">
      <p class="text-muted-foreground">
       Or click below to login from this device.
      </p>
        <a class="uk-btn uk-btn-default" href="#" >
          Login with Portal
        </a>
    </div>
  </div>
</div>
</div>
