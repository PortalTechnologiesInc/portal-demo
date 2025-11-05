<script>
  import Theme from './lib/Theme.svelte'
  import Login from './lib/Login.svelte'
  import { count, loggedIn} from './state.svelte.js';
  import { onMount } from 'svelte';
  import { connected , messages, errors} from './socket.svelte.js';
  onMount(() => {
    count.set(245);

  })
</script>

{#if $connected}
    <!-- {#each $messages as msg}
    <div class="uk-alert" data-uk-alert>
      <a href class="uk-alert-close" data-uk-close></a>
      <p>
        {JSON.stringify(msg)}
      </p>
    </div>
    {/each} -->
  {#each $errors as error}
    <div class="uk-alert uk-alert-destructive" data-uk-alert>
      <a href class="uk-alert-close" data-uk-close></a>
      <p>
        {error}
      </p>
    </div>
  {/each}
  {#if $loggedIn}
    <div class="uk-alert uk-alert-success" data-uk-alert>
      <a href class="uk-alert-close" data-uk-close></a>
      <p>
        Logged in
      </p>
    </div>
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




