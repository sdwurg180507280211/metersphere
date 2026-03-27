import user from 'metersphere-frontend/src/store/modules/user';
import {defineStore} from 'pinia';
import apiState from './state';
import advancedSearch from './modules/advancedSearch';

let useApiStore = defineStore(apiState);
let useUserStore = defineStore(user);
let useAdvancedSearchStore = defineStore(advancedSearch);

const useStore = () => ({
  user: useUserStore(),
  api: useApiStore(),
  advancedSearch: useAdvancedSearchStore(),
});

export {
  useUserStore,
  useApiStore,
  useAdvancedSearchStore,
  useStore as default
};
