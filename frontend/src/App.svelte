<script>
  import Theme from './lib/Theme.svelte'
  import Login from './lib/Login.svelte'
  import { count, loggedIn, profile} from './state.svelte.js';
  import { onMount } from 'svelte';
  import { connected , messages, errors} from './socket.svelte.js';
  import Dashboard from './lib/Dashboard.svelte';
  onMount(() => {
    count.set(245);

  })

  $: if ($errors.length > 0) {
    let lastError = $errors[$errors.length - 1];
    UIkit.notification(lastError, { status: 'destructive' });
  }
</script>

<Theme />
{#if $connected}
    <!-- {#each $messages as msg}
    <div class="uk-alert" data-uk-alert>
      <a href class="uk-alert-close" data-uk-close></a>
      <p>
        {JSON.stringify(msg)}
      </p>
    </div>
    {/each} -->
  <!-- {#each $errors as error}
    <div class="uk-alert uk-alert-destructive" data-uk-alert>
      <a href class="uk-alert-close" data-uk-close></a>
      <p>
        {error}
      </p>
    </div>
  {/each} -->
  {#if $loggedIn}
    <Dashboard />
  {/if}
  {#if !$loggedIn}
    <Login />
  {/if}
{:else}
  <div class="uk-alert uk-alert-destructive" data-uk-alert>
    <a href class="uk-alert-close" data-uk-close></a>
    <p>
      Not connected to the server
    </p>
  </div>
{/if}




